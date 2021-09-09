package com.cisco.demo.jtapi.makecall;

/**
 * TraceWindow.java
 * 
 * Copyright Cisco Systems, Inc.
 * 
 */

import java.awt.*;
import java.awt.event.*;

@SuppressWarnings("serial")
public class TraceWindow extends Frame implements Trace {

	TextArea textArea;
	boolean traceEnabled = true;
	StringBuffer buffer = new StringBuffer();

	public TraceWindow(String name) {
		super(name);
		initWindow();
	}

	public TraceWindow() {
		this("");
	}

	private void initWindow() {
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		textArea = new TextArea();
		setSize(400, 400);
		add(textArea);
		setEnabled(true);
		this.setVisible(true);
	}

	public final void bufPrint(String str) {
		if (traceEnabled) {
			buffer.append(str);
		}
	}

	public final void print(String str) {
		if (traceEnabled) {
			buffer.append(str);
			flush();
		}
	}

	public final void print(char character) {
		if (traceEnabled) {
			buffer.append(character);
			flush();
		}
	}

	public final void print(int integer) {
		if (traceEnabled) {
			buffer.append(integer);
			flush();
		}
	}

	public final void println(String str) {
		if (traceEnabled) {
			print(str);
			print('\n');
			flush();
		}
	}

	public final void println(char character) {
		if (traceEnabled) {
			print(character);
			print('\n');
			flush();
		}
	}

	public final void println(int integer) {
		if (traceEnabled) {
			print(integer);
			print('\n');
			flush();
		}
	}

	public final void setTrace(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
	}

	public final void flush() {
		if (traceEnabled) {
			textArea.append(buffer.toString());
			buffer = new StringBuffer();
		}
	}

	public final void clear() {

		textArea.setText("");
	}
}