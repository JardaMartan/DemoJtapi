package com.cisco.jmartan.demo.jtapi;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.telephony.Address;
import javax.telephony.CallObserver;
import javax.telephony.Connection;
import javax.telephony.InvalidStateException;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.Terminal;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;

import com.cisco.cti.util.Condition;
import com.cisco.jtapi.extensions.CiscoJtapiPeer;
import com.cisco.jtapi.extensions.CiscoJtapiVersion;
import com.cisco.jtapi.extensions.CiscoProvider;
import com.cisco.jtapi.extensions.CiscoProviderObserver;
import com.cisco.jtapi.extensions.CiscoTerminal;

public class PhonePush implements CiscoProviderObserver {
	Condition conditionInService = new Condition();
	CiscoProvider provider;
	Address[] addr;
	CallObserver observer;

// Key:Settings
	String[] keySequence6900 = new String[] { "Settings", "KeyPad4", "KeyPad5",
			"Soft4", "Soft2" };
	String[] keySequence7900 = new String[] { "Settings", "KeyPad4", "KeyPad5",
			"KeyPadStar", "KeyPadStar", "KeyPadPound", "Soft4", "Soft2" };
	String[] keySequence9900 = new String[] { "Applications", "KeyPad4",
			"KeyPad4", "KeyPad4", "Soft3" };
	Integer[] series6900 = { CiscoTerminal.DEVICETYPE_CISCO_6911,
			CiscoTerminal.DEVICETYPE_CISCO_6921,
			CiscoTerminal.DEVICETYPE_CISCO_6941,
			CiscoTerminal.DEVICETYPE_CISCO_6961,
			CiscoTerminal.DEVICETYPE_CISCO_6945 };
	Integer[] series7900 = { CiscoTerminal.DEVICETYPE_CISCO_7941,
			CiscoTerminal.DEVICETYPE_CISCO_7941G_GE,
			CiscoTerminal.DEVICETYPE_CISCO_7942,
			CiscoTerminal.DEVICETYPE_CISCO_7945,
			CiscoTerminal.DEVICETYPE_CISCO_7961,
			CiscoTerminal.DEVICETYPE_CISCO_7961G_GE,
			CiscoTerminal.DEVICETYPE_CISCO_7962,
			CiscoTerminal.DEVICETYPE_CISCO_7965 };
	Integer[] series9900 = { CiscoTerminal.DEVICETYPE_CISCO_9971,
			CiscoTerminal.DEVICETYPE_9951, CiscoTerminal.DEVICETYPE_CISCO_8961 };

	private static Integer KEY_DELAY = 500; // delay between key presses
	private static final int THREAD_LIMIT = 10; // maximum number of running
// requests

	private class PressThread implements Runnable {
		private CiscoTerminal terminal;

		public PressThread(CiscoTerminal terminal) {
			this.terminal = terminal;
		}

		private void keyPress(String[] keySequence)
				throws InvalidStateException, MethodNotSupportedException,
				InterruptedException {
			Integer delayMultiplier = 1;
			if (Arrays.asList(series6900).contains(this.terminal.getType())) {
				delayMultiplier = 3; // 6900 series is slow
			}
			for (int k = 0; k < keySequence.length; k++) {
				String exec = "<CiscoIPPhoneExecute><ExecuteItem Priority=\"0\" URL=\"Key:"
						+ keySequence[k] + "\"/></CiscoIPPhoneExecute>";
				this.terminal.sendData(exec.getBytes());
				Thread.sleep(delayMultiplier * KEY_DELAY);
			}
		}

		@Override
		public void run() {
			try {
				if (Arrays.asList(series6900).contains(this.terminal.getType())) {
					System.out.println("6900 series");
					keyPress(keySequence6900);
				} else if (Arrays.asList(series7900).contains(
						this.terminal.getType())) {
					System.out.println("7900 series");
					keyPress(keySequence7900);
				} else if (Arrays.asList(series9900).contains(
						this.terminal.getType())) {
					System.out.println("9900 series");
					keyPress(keySequence9900);
				} else {
					System.out.println("Phone model "
							+ this.terminal.getTypeName() + " not found");
				}
			} catch (Exception e) {
				System.out.println("Press exception: " + e.getMessage());
			}
		}
	}

	public PhonePush(String args[]) {
		System.out.println("phonepush: " + new CiscoJtapiVersion());
		try {
			System.out.println("Initializing Jtapi");
			int curArg = 0;
			String providerName = args[curArg++];
			String login = args[curArg++];
			String passwd = args[curArg++];
			CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory
					.getJtapiPeer(null);
			String providerString = providerName + ";login=" + login
					+ ";passwd=" + passwd;
			System.out.println("Opening: " + providerString);
			provider = (CiscoProvider) peer.getProvider(providerString);
			provider.addObserver(this);
			conditionInService.waitTrue();

			observer = new CallObserver() {

				@Override
				public void callChangedEvent(CallEv[] eventList) {
					for (int i = 0; i < eventList.length; i++) {
						CallEv curEv = eventList[i];
						System.out.println("Event: " + curEv.toString());
						Connection con = null;
						try {
							if (curEv instanceof ConnConnectedEv) {
								con = ((ConnConnectedEv) curEv).getConnection();
								System.out.println("Connected: "
										+ con.toString());
							}
							if (curEv instanceof ConnDisconnectedEv) {
								con = ((ConnDisconnectedEv) curEv)
										.getConnection();
								System.out.println("Disonnected: "
										+ con.toString());
							}
						} catch (Exception e) {
							System.out.println("Caught exception " + e);
							System.out.println("Connection = " + con);
						}
					}
				}
			};

			addr = provider.getAddresses();
			for (int i = 0; i < addr.length; i++) {
				addr[i].addCallObserver(observer);
				System.out.println("Added observer to: " + addr[i].getName());
			}

			ExecutorService executor = Executors
					.newFixedThreadPool(THREAD_LIMIT);

			for (int i = 0; i < addr.length; i++) {
				Terminal[] term = addr[i].getTerminals();

				System.out.print("Pushing to: " + addr[i].getName() + " - ");
				for (int j = 0; j < term.length; j++) {
// Thread.sleep(2000);
					CiscoTerminal t = (CiscoTerminal) term[j];
					System.out
							.print(t.getName() + "(" + t.getTypeName() + ") ");
					// System.out.println("State: " + t.getState());
// thread here
					Runnable phonePress = new PressThread(t);
					executor.execute(phonePress);
				}
				executor.shutdown();
				while (!executor.isTerminated()) {
				}
				System.out.println();
				System.out.println("Finished all threads");
			}

			System.out.println("Done");

		} catch (Exception e) {
			System.out.println("Caught exception " + e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Usage: phonepush <server> <login> <password>");
			System.exit(1);
		}
		new PhonePush(args);
		System.exit(0);
	}

	@Override
	public void providerChangedEvent(ProvEv[] eventList) {
		if (eventList != null) {
			for (int i = 0; i < eventList.length; i++) {
				if (eventList[i] instanceof ProvInServiceEv) {
					conditionInService.set();
				}
			}
		}
	}

}
