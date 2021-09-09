package com.cisco.demo.jtapi.makecall;

/**
 * Trace.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 */
public interface Trace {
	/**
	 * bufPrint (str) puts str in buffer only.
	 */
	public void bufPrint(String string);

	/**
	 * print () println () bufPrint and invoke flush ();
	 */
	public void print(String string);

	public void print(char character);

	public void print(int integer);

	public void println(String string);

	public void println(char character);

	public void println(int integer);

	/**
	 * flush out the buffer.
	 */
	public void flush();
}