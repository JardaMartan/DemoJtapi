package com.cisco.demo.jtapi.makecall;

/**
 * StopSignal.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 */

class StopSignal {
	boolean stopping = false;
	boolean stopped = false;

	synchronized boolean isStopped() {
		return stopped;
	}

	synchronized boolean isStopping() {
		return stopping;
	}

	synchronized void stop() {
		if (!stopped) {
			stopping = true;
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
	}

	synchronized void canStop() {
		if (stopping = true) {
			stopping = false;
			stopped = true;
			notify();
		}
	}
}