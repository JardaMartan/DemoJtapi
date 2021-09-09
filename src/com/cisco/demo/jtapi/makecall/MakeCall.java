package com.cisco.demo.jtapi.makecall;

/**
 * makecall.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 * Performance-testing application (first pass) for Cisco JTAPI
 * implementation.
 * 
 * Known problems:
 * 
 * Due to synchronization problems between Actors, calls may
 * not be cleared when this application shuts down.
 * 
 */

//import com.ms.wfc.app.*;
import java.util.*;
import javax.telephony.*;
import javax.telephony.events.*;
import com.cisco.cti.util.Condition;
import com.cisco.jtapi.extensions.CiscoJtapiVersion;

@SuppressWarnings("serial")
public class MakeCall extends TraceWindow implements ProviderObserver {
	Vector<Actor> actors = new Vector<Actor>();
	Condition conditionInService = new Condition();
	Provider provider;

	public MakeCall(String[] args) {
		super("makecall" + ": " + new CiscoJtapiVersion());
		try {
			println("Initializing Jtapi");
			int curArg = 0;
			String providerName = args[curArg++];
			String login = args[curArg++];
			String passwd = args[curArg++];
			int actionDelayMillis = Integer.parseInt(args[curArg++]);
			String src = null;
			String dest = null;
			JtapiPeer peer = JtapiPeerFactory.getJtapiPeer(null);
			if (curArg < args.length) {
				String providerString = providerName + ";login=" + login
						+ ";passwd=" + passwd;
				println("Opening " + providerString + "...\n");
				provider = peer.getProvider(providerString);
				provider.addObserver(this);
				conditionInService.waitTrue();
				println("Constructing actors");

				for (; curArg < args.length; curArg++) {
					if (src == null) {
						src = args[curArg];
					} else {
						dest = args[curArg];
						Originator originator = new Originator(
								provider.getAddress(src), dest, this,
								actionDelayMillis);
						actors.addElement(originator);
						actors.addElement(new Receiver(provider
								.getAddress(dest), this, actionDelayMillis,
								originator));
						src = null;
						dest = null;
					}
				}
				if (src != null) {
					println("Skipping last originating address \"" + src
							+ "\"; no destination specified");
				}

			}
			Enumeration<Actor> e = actors.elements();
			while (e.hasMoreElements()) {
				Actor actor = (Actor) e.nextElement();
				actor.initialize();
			}

			Enumeration<Actor> en = actors.elements();
			while (en.hasMoreElements()) {
				Actor actor = (Actor) en.nextElement();
				actor.start();
			}
		} catch (Exception e) {
			println("Caught exception " + e);
		}
	}

	public void dispose() {
		println("Stopping actors");
		Enumeration<Actor> e = actors.elements();
		while (e.hasMoreElements()) {
			Actor actor = (Actor) e.nextElement();
			actor.dispose();
		}
	}

	public static void main ( String [] args )
	{
		if ( args.length < 6 ) {
			System.out.println ( "Usage: makecall <server> <login> <password> <delay> <origin> <destination> ..." );
			System.exit ( 1 );
		}
		new MakeCall ( args );
	}

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