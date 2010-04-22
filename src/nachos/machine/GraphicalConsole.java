// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import machine.Lib;
import nachos.security.*;

import hardware.SerialConsole;

import java.lang.reflect.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * A graphical console that uses the AWT to put a console in a window.
 */
public final class GraphicalConsole implements SerialConsole {
	/**
	 * Allocate a new graphical console.
	 */
	public GraphicalConsole() {
		System.out.print(" gconsole");
	}

	public void init(Privilege privilege) {
		this.privilege = privilege;

		receiveInterrupt = new Runnable() {
			public void run() {
				receiveInterrupt();
			}
		};

		sendInterrupt = new Runnable() {
			public void run() {
				sendInterrupt();
			}
		};

		textArea = new JTextArea(25, 80);
		textArea.setEditable(false);
		textArea.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				GraphicalConsole.this.keyPressed(e);
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyTyped(KeyEvent e) {
			}
		});
		textArea.setLineWrap(true);

		scrollPane = new JScrollPane(textArea);

		frame = new JFrame("Nachos console");
		frame.getContentPane().add(scrollPane);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				Machine.terminate();
			}

			public void windowActivated(WindowEvent e) {
				textArea.append("");// a\nb\nc\nd\ne\nf\n");
			}
		});

		frame.pack();
		frame.show();

		incomingQueue = new LinkedList();
		incomingKey = -1;
		scheduleReceiveInterrupt();

		outgoingKey = -1;
	}

	public void setInterruptHandlers(Runnable receiveInterruptHandler,
			Runnable sendInterruptHandler) {
		this.receiveInterruptHandler = receiveInterruptHandler;
		this.sendInterruptHandler = sendInterruptHandler;
	}

	private void keyPressed(KeyEvent e) {
		char c = e.getKeyChar();
		if (c == 0x03)
			frame.dispose();

		if ((c < 0x20 || c >= 0x80) && c != '\t' && c != '\n' && c != '\b'
				&& c != 0x1B)
			return;

		synchronized (incomingQueue) {
			System.out.println("queuing " + c);
			incomingQueue.add(new Integer((int) c));
		}
	}

	private void scheduleReceiveInterrupt() {
		privilege.interrupt.schedule(Stats.ConsoleTime, "console read",
				receiveInterrupt);
	}

	private void receiveInterrupt() {
		Lib.assert_(incomingKey == -1);

		synchronized (incomingQueue) {
			if (incomingQueue.isEmpty()) {
				scheduleReceiveInterrupt();
			} else {
				Integer i = (Integer) incomingQueue.removeFirst();
				incomingKey = i.intValue();

				privilege.stats.numConsoleReads++;
				if (receiveInterruptHandler != null)
					receiveInterruptHandler.run();
			}
		}
	}

	public int readByte() {
		int key = incomingKey;

		if (incomingKey != -1) {
			incomingKey = -1;
			scheduleReceiveInterrupt();
		}

		return key;
	}

	private void scheduleSendInterrupt() {
		privilege.interrupt.schedule(Stats.ConsoleTime, "console write",
				sendInterrupt);
	}

	private void sendInterrupt() {
		Lib.assert_(outgoingKey != -1);

		Runnable send = new Runnable() {
			public void run() {
				textArea.append("" + (char) outgoingKey);
			}
		};

		try {
			SwingUtilities.invokeAndWait(send);
		} catch (InvocationTargetException e) {
			Machine.terminate(e.getTargetException());
		} catch (Exception e) {
			Machine.terminate(e);
		}

		outgoingKey = -1;
		if (sendInterruptHandler != null)
			sendInterruptHandler.run();
	}

	public void writeByte(int value) {
		if (outgoingKey == -1)
			scheduleSendInterrupt();
		outgoingKey = value & 0xFF;
	}

	private Privilege privilege;

	private Runnable receiveInterrupt;
	private Runnable sendInterrupt;

	private Runnable receiveInterruptHandler = null;
	private Runnable sendInterruptHandler = null;

	private JFrame frame;
	private JTextArea textArea;
	private JScrollPane scrollPane;

	private LinkedList incomingQueue;
	private int incomingKey;
	private int outgoingKey;
}
