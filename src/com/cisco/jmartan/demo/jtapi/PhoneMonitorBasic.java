package com.cisco.jmartan.demo.jtapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.telephony.Address;
import javax.telephony.CallObserver;
import javax.telephony.Connection;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.Provider;
import javax.telephony.ProviderObserver;
import javax.telephony.Terminal;
import javax.telephony.events.CallEv;
import javax.telephony.events.ConnConnectedEv;
import javax.telephony.events.ConnDisconnectedEv;
import javax.telephony.events.ProvEv;
import javax.telephony.events.ProvInServiceEv;

import com.cisco.cti.util.Condition;
import com.cisco.jtapi.extensions.CiscoJtapiVersion;

public class PhoneMonitorBasic implements ProviderObserver {
	Condition conditionInService = new Condition();
	Provider provider;
	Address addr;
	CallObserver observer;

	public PhoneMonitorBasic(String args[]) {
		System.out.println("phonemonitor: " + new CiscoJtapiVersion());
		try {
			System.out.println("Initializing Jtapi");
			int curArg = 0;
			String providerName = args[curArg++];
			String login = args[curArg++];
			String passwd = args[curArg++];
			String line = args[curArg++];
			JtapiPeer peer = JtapiPeerFactory
					.getJtapiPeer(null);
			String providerString = providerName + ";login=" + login
					+ ";passwd=" + passwd;
			System.out.println("Opening: " + providerString);
			provider = peer.getProvider(providerString);
			provider.addObserver(this);
			conditionInService.waitTrue();

			addr = provider.getAddress(line);
			Terminal[] term = addr.getTerminals();

			System.out.print("Monitoring: " + addr.getName() + " - ");
			for (int i = 0; i < term.length; i++) {
				System.out.print(term[i].getName() + " ");
			}
			System.out.println();

			observer = new CallObserver() {

				@Override
				public void callChangedEvent(CallEv[] eventList) {
					for (int i = 0; i < eventList.length; i++) {
						CallEv curEv = eventList[i];
						System.out.println("Event: " + curEv.toString());
						Connection con = null;
						try {
							if (curEv instanceof ConnConnectedEv) {
								con = ((ConnConnectedEv) curEv)
										.getConnection();
								System.out
										.println("Connected: " + con.toString());
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
		new PhoneMonitorBasic(args);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.println("Press enter to exit...");
			String s = null;
			try {
				s = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (s.length() == 0) {
				System.out.println("Done");
				System.exit(0);
			}
		}
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
