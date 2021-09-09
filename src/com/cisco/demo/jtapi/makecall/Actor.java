package com.cisco.demo.jtapi.makecall;

/**
 * Actor.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 */

import javax.telephony.Address;
import javax.telephony.AddressObserver;
import javax.telephony.Terminal;
import javax.telephony.TerminalObserver;
import javax.telephony.callcontrol.CallControlCallObserver;
import javax.telephony.events.AddrEv;
import javax.telephony.events.CallEv;
import javax.telephony.events.TermEv;

import com.cisco.jtapi.extensions.CiscoAddrInServiceEv;
import com.cisco.jtapi.extensions.CiscoAddrOutOfServiceEv;
import com.cisco.jtapi.extensions.CiscoTermInServiceEv;
import com.cisco.jtapi.extensions.CiscoTermOutOfServiceEv;

public abstract class Actor implements AddressObserver, TerminalObserver,
		CallControlCallObserver, Trace {
	public static final int ACTOR_OUT_OF_SERVICE = 0;
	public static final int ACTOR_IN_SERVICE = 1;
	private Trace trace;
	protected int actionDelayMillis;
	private Address observedAddress;
	private Terminal observedTerminal;
	private boolean addressInService;
	private boolean terminalInService;
	protected int state = Actor.ACTOR_OUT_OF_SERVICE;

	public Actor(Trace trace, Address observed, int actionDelayMillis) {
		this.trace = trace;
		this.observedAddress = observed;
		this.observedTerminal = observed.getTerminals()[0];
		this.actionDelayMillis = actionDelayMillis;
	}

	public void initialize() {
		try {
			if (observedAddress != null) {
				bufPrintln("Adding Call observer to address "
						+ observedAddress.getName());
				observedAddress.addCallObserver(this);
				// Now add observer on Address and Terminal
				bufPrintln("Adding Adddress Observer to address "
						+ observedAddress.getName());
				observedAddress.addObserver(this);
				bufPrintln("Adding Terminal Observer to Terminal"
						+ observedTerminal.getName());
				observedTerminal.addObserver(this);
			}
		} catch (Exception e) {
		} finally {
			flush();
		}
	}

	public final void start() {
		onStart();
	}

	public final void dispose() {
		try {
			onStop();
			if (observedAddress != null) {
				bufPrintln("Removing observer from Address "
						+ observedAddress.getName());
				observedAddress.removeObserver(this);
				bufPrintln("Removing call observer from Address "
						+ observedAddress.getName());
				observedAddress.removeCallObserver(this);
			}
			if (observedTerminal != null) {
				bufPrintln("Removing observer from terminal "
						+ observedTerminal.getName());
				observedTerminal.removeObserver(this);
			}
		} catch (Exception e) {
			println("Caught exception " + e);
		} finally {
			flush();
		}
	}

	public final void stop() {
		onStop();
	}

	public final void callChangedEvent(CallEv[] events) {
		//
		// for now, all metaevents are delivered in the
		// same package...
		//
		metaEvent(events);
	}

	public void addressChangedEvent ( AddrEv [] events ) {
		for ( int i=0; i<events.length; i++ ) {
			Address address = events[i].getAddress ();
			switch ( events[i].getID () ) {				
				case CiscoAddrInServiceEv.ID:
					 bufPrintln ( "Received " + events[i] + "for "+ address.getName ());	
					 addressInService = true;	
					 if ( terminalInService ) {
						if ( state != Actor.ACTOR_IN_SERVICE ) {
							state = Actor.ACTOR_IN_SERVICE ;
							fireStateChanged ();
						}
					 }					 
	  	   	 break;					
				case CiscoAddrOutOfServiceEv.ID:					 					
					 bufPrintln ( "Received " + events[i] + "for "+ address.getName ());						 
					 addressInService = false;
 					 if ( state != Actor.ACTOR_OUT_OF_SERVICE ) {
						state = Actor.ACTOR_OUT_OF_SERVICE; // you only want to notify when you had notified earlier that you are IN_SERVICE
						fireStateChanged ();
					 }					 
					break;
			}
		}
		flush ();		
	}

	public void terminalChangedEvent ( TermEv [] events ) {
		for ( int i=0; i<events.length; i++ ) {
			Terminal terminal = events[i].getTerminal ();
			switch ( events[i].getID () ) {								
				case CiscoTermInServiceEv.ID:
					 bufPrintln ( "Received " + events[i] + "for " + terminal.getName ());					
					 terminalInService = true;	
					 if ( addressInService ) {
						if ( state != Actor.ACTOR_IN_SERVICE ) {
							state = Actor.ACTOR_IN_SERVICE;
							fireStateChanged ();
						}					 
					 }
					 break;
				case CiscoTermOutOfServiceEv.ID:
					 bufPrintln ( "Received " + events[i] + "for " + terminal.getName () );						 
					 terminalInService = false;
					 if ( state != Actor.ACTOR_OUT_OF_SERVICE ) { // you only want to notify when you had notified earlier that you are IN_SERVICE
						state = Actor.ACTOR_OUT_OF_SERVICE;
						fireStateChanged ();
					 }								 
					 break;
			}
		}
		flush();		
	}

	final void delay(String action) {
		if (actionDelayMillis != 0) {
			println("Pausing " + actionDelayMillis + " milliseconds before "
					+ action);
			try {
				Thread.sleep(actionDelayMillis);
			} catch (InterruptedException e) {
			}
		}
	}

	protected abstract void metaEvent(CallEv[] events);

	protected abstract void onStart();

	protected abstract void onStop();

	protected abstract void fireStateChanged();

	public final void bufPrint(String string) {
		trace.bufPrint(string);
	}

	public final void bufPrintln(String string) {
		trace.bufPrint(string);
		trace.bufPrint("\n");
	}

	public final void print(String string) {
		trace.print(string);
	}

	public final void print(char character) {
		trace.print(character);
	}

	public final void print(int integer) {
		trace.print(integer);
	}

	public final void println(String string) {
		trace.println(string);
	}

	public final void println(char character) {
		trace.println(character);
	}

	public final void println(int integer) {
		trace.println(integer);
	}

	public final void flush() {
		trace.flush();
	}
}