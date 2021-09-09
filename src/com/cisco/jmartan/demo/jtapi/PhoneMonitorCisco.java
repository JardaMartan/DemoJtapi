package com.cisco.jmartan.demo.jtapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.telephony.CallObserver;
import javax.telephony.InvalidStateException;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Terminal;
import javax.telephony.callcontrol.CallControlCall;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;
import javax.telephony.events.TermEv;

import com.cisco.cti.util.Condition;
import com.cisco.jtapi.extensions.CiscoAddress;
import com.cisco.jtapi.extensions.CiscoConnection;
import com.cisco.jtapi.extensions.CiscoJtapiPeer;
import com.cisco.jtapi.extensions.CiscoJtapiVersion;
import com.cisco.jtapi.extensions.CiscoPickupGroup;
import com.cisco.jtapi.extensions.CiscoProvider;
import com.cisco.jtapi.extensions.CiscoProviderObserver;
import com.cisco.jtapi.extensions.CiscoRTPInputProperties;
import com.cisco.jtapi.extensions.CiscoRTPInputStartedEv;
import com.cisco.jtapi.extensions.CiscoRTPOutputProperties;
import com.cisco.jtapi.extensions.CiscoRTPOutputStartedEv;
import com.cisco.jtapi.extensions.CiscoTermButtonPressedEv;
import com.cisco.jtapi.extensions.CiscoTermEv;
import com.cisco.jtapi.extensions.CiscoTermEvFilter;
import com.cisco.jtapi.extensions.CiscoTerminal;
import com.cisco.jtapi.extensions.CiscoTerminalObserver;

public class PhoneMonitorCisco implements CiscoProviderObserver {
	Condition conditionInService = new Condition();
	Condition terminalInService = new Condition();
	CiscoProvider provider;
	static CiscoAddress addr;
	CallObserver observer;
	Terminal[] terminalArr;
	String line;
	static CiscoTerminal monitoredTerminal;

	public PhoneMonitorCisco(String args[]) {
		System.out.println("phonemonitor: " + new CiscoJtapiVersion());
		try {
			System.out.println("Initializing Jtapi");
			int curArg = 0;
			String providerName = args[curArg++];
			String login = args[curArg++];
			String passwd = args[curArg++];
			line = args[curArg++];
			CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory
					.getJtapiPeer(null);
			String providerString = providerName + ";login=" + login
					+ ";passwd=" + passwd;
			System.out.println("Opening: " + providerString);
			provider = (CiscoProvider) peer.getProvider(providerString);
			provider.addObserver(this);
			conditionInService.waitTrue();

			addr = (CiscoAddress) provider.getAddress(line);
			terminalArr = addr.getTerminals();
			CiscoPickupGroup pickup = addr.getPickupGroup();
			String pickupDN = pickup.getPickupGroupDN();
			System.out.println("Pickup Group DN: " + pickupDN);

			System.out.print("Monitoring: " + addr.getName() + " - ");
			for (int i = 0; i < terminalArr.length; i++) {
				System.out.print(terminalArr[i].getName() + " ");
				monitoredTerminal = (CiscoTerminal) terminalArr[i];
				monitoredTerminal.addObserver(new MyTerminalObserver());
				CiscoTermEvFilter f = monitoredTerminal.getFilter();
				f.setButtonPressedEnabled(true);
				f.setDeviceDataEnabled(true);
				f.setDeviceStateActiveEvFilter(true);
				f.setDeviceStateAlertingEvFilter(true);
				f.setDeviceStateHeldEvFilter(true);
				f.setDeviceStateIdleEvFilter(true);
				f.setDeviceStateWhisperEvFilter(true);
				f.setDNDChangedEvFilter(true);
				f.setDNDOptionChangedEvFilter(true);
				f.setRTPEventsEnabled(true);
				f.setRTPKeyEventsEnabled(true);
				f.setSnapshotEnabled(true);
				monitoredTerminal.setFilter(f);
			}
			System.out.println();

			observer = new CallObserver() {

				@Override
				public void callChangedEvent(CallEv[] eventList) {
					for (int i = 0; i < eventList.length; i++) {
						CallEv curEv = eventList[i];
						System.out.println("Event: " + curEv.toString());
						CiscoConnection con = null;
						try {
							if (curEv instanceof ConnConnectedEv) {
								con = (CiscoConnection) ((ConnConnectedEv) curEv)
										.getConnection();
								System.out.println("Connected: "
										+ con.toString());
								CallControlCall cc = (CallControlCall) con
										.getCall();
								System.out.println("  call: " + cc.toString());
							}
							if (curEv instanceof ConnDisconnectedEv) {
								con = (CiscoConnection) ((ConnDisconnectedEv) curEv)
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
			addr.addCallObserver(observer);

		} catch (Exception e) {
			System.out.println("Caught exception " + e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out
					.println("Usage: phonemonitor <server> <login> <password> <line>");
			System.exit(1);
		}
		new PhoneMonitorCisco(args);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.println("Press P<Enter> for pickup, <Enter> for exit...");
			String s = null;
			try {
				s = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (s.length() == 0) {
				System.out.println("Done");
				System.exit(0);
			} else {
				if (s.equalsIgnoreCase("p")) {
					try {
						System.out.println("*** PICKUP ***");
						monitoredTerminal.pickup(addr);
					} catch (InvalidStateException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void providerChangedEvent(ProvEv[] eventList) {
		if (eventList != null) {
			for (int i = 0; i < eventList.length; i++) {
				System.out
						.println("Provider event: " + eventList[i].toString());
				if (eventList[i] instanceof ProvInServiceEv) {
					conditionInService.set();
				}
			}
		}
	}

	private class MyTerminalObserver implements CiscoTerminalObserver {

		@Override
		public void terminalChangedEvent(TermEv[] tEventList) {
			for (int i = 0; i < tEventList.length; i++) {
				CiscoTermEv curEv = (CiscoTermEv) tEventList[i];
				System.out.print("Terminal event: " + curEv.toString());
				if (curEv instanceof CiscoTermButtonPressedEv) {
					System.out.print(" - button "
							+ ((CiscoTermButtonPressedEv) curEv)
									.getButtonPressed());
				}
				if (curEv instanceof CiscoRTPOutputStartedEv) {
					CiscoRTPOutputProperties p = ((CiscoRTPOutputStartedEv) curEv)
							.getRTPOutputProperties();
					System.out.print(" - rtp out "
							+ p.getRemoteAddress().toString() + ":"
							+ p.getRemotePort() + " " + p.getPayloadType());
				}
				if (curEv instanceof CiscoRTPInputStartedEv) {
					CiscoRTPInputProperties p = ((CiscoRTPInputStartedEv) curEv)
							.getRTPInputProperties();
					System.out.print(" - rtp in "
							+ p.getLocalAddress().toString() + ":"
							+ p.getLocalPort() + " " + p.getPayloadType());
				}
				System.out.println();
			}
		}

	}
}
