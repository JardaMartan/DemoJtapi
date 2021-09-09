package com.cisco.demo.jtapi.makecall;

/**
 * originator.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 */

import javax.telephony.Address;
import javax.telephony.Call;
import javax.telephony.Connection;
import javax.telephony.InvalidArgumentException;
import javax.telephony.InvalidPartyException;
import javax.telephony.InvalidStateException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.PrivilegeViolationException;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.TerminalConnection;
import javax.telephony.callcontrol.events.CallCtlConnDisconnectedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnTalkingEv;
import javax.telephony.events.CallEv;


public class Originator extends Actor {
	Address srcAddress;
	String destAddress;
	int iteration;
	StopSignal stopSignal;
	boolean ready = false;
	int receiverState = Actor.ACTOR_OUT_OF_SERVICE;
	boolean callInIdle = true;

	public Originator(Address srcAddress, String destAddress, Trace trace,
			int actionDelayMillis) {
		super(trace, srcAddress, actionDelayMillis); // observe srcAddress
		this.srcAddress = srcAddress;
		this.destAddress = destAddress;
		this.iteration = 0;
	}

	protected final void metaEvent(CallEv[] eventList) {
		for (int i = 0; i < eventList.length; i++) {
			try {
				CallEv curEv = eventList[i];
				if (curEv instanceof CallCtlTermConnTalkingEv) {
					TerminalConnection tc = ((CallCtlTermConnTalkingEv) curEv)
							.getTerminalConnection();
					Connection conn = tc.getConnection();
					if (conn.getAddress().getName().equals(destAddress)) {
						delay("disconnecting");
						bufPrintln("Disconnecting Connection " + conn);
						conn.disconnect();
					}
				} else if (curEv instanceof CallCtlConnDisconnectedEv) {
					Connection conn = ((CallCtlConnDisconnectedEv) curEv)
							.getConnection();
					if (conn.getAddress().equals(srcAddress)) {
						stopSignal.canStop();
						setCallProgressState(true);
					}
				}
			} catch (Exception e) {
				println("Caught exception " + e);
			} finally {
				flush();
			}
		}
	}

	protected void makecall() throws ResourceUnavailableException,
			InvalidStateException, PrivilegeViolationException,
			MethodNotSupportedException, InvalidPartyException,
			InvalidArgumentException {
		println("Making call #" + ++iteration + " from " + srcAddress + " to "
				+ destAddress + " " + Thread.currentThread().getName());
		Call call = srcAddress.getProvider().createCall();
		call.connect(srcAddress.getTerminals()[0], srcAddress, destAddress);
		setCallProgressState(false);
		println("Done making call");
	}

	protected final void onStart() {
		stopSignal = new StopSignal();
		new ActionThread().start();
	}

	protected final void fireStateChanged() {
		checkReadyState();
	}

	protected final void onStop() {
		stopSignal.stop();
		Connection[] connections = srcAddress.getConnections();
		try {
			if (connections != null) {
				for (int i = 0; i < connections.length; i++) {
					connections[i].disconnect();
				}
			}
		} catch (Exception e) {
			println(" Caught Exception " + e);
		}
	}

	public int getReceiverState() {
		return receiverState;
	}

	public void setReceiverState(int state) {
		if (receiverState != state) {
			receiverState = state;
			checkReadyState();
		}
	}

	public synchronized void checkReadyState() {
		if (receiverState == Actor.ACTOR_IN_SERVICE
				&& state == Actor.ACTOR_IN_SERVICE) {
			ready = true;
		} else {
			ready = false;
		}
		notifyAll();
	}

	public synchronized void setCallProgressState(boolean isCallInIdle) {
		callInIdle = isCallInIdle;
		notifyAll();
	}

	public synchronized void doAction() {
		if (!ready || !callInIdle) {
			try {
				wait();
			} catch (Exception e) {
				println(" Caught Exception from wait state" + e);
			}
		} else {
			if (actionDelayMillis != 0) {
				println("Pausing " + actionDelayMillis
						+ " milliseconds before making call ");
				flush();
				try {
					wait(actionDelayMillis);
				} catch (Exception ex) {
				}
			}
			// make call after waking up, recheck the flags before making the
			// call
			if (ready && callInIdle) {
				try {
					makecall();
				} catch (Exception e) {
					println(" Caught Exception in MakeCall " + e + " Thread ="
							+ Thread.currentThread().getName());
				}
			}
		}
	}

	class ActionThread extends Thread {
		ActionThread() {
			super("ActionThread");
		}

		public void run() {
			while (true) {
				doAction();
			}
		}
	}
}