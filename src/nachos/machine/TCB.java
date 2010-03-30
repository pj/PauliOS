// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import machine.Lib;
import nachos.security.*;
import nachos.threads.KThread;

import java.util.Vector;
import java.security.PrivilegedAction;

/**
 * A TCB simulates the low-level details necessary to create, context-switch,
 * and destroy Nachos threads. Each TCB controls an underlying JVM Thread
 * object.
 * 
 * <p>
 * Do not use any methods in <tt>java.lang.Thread</tt>, as they are not
 * compatible with the TCB API. Most <tt>Thread</tt> methods will either crash
 * Nachos or have no useful effect.
 * 
 * <p>
 * Do not use the <i>synchronized</i> keyword <b>anywhere</b> in your code. It's
 * against the rules, <i>and</i> it can easily deadlock nachos.
 */
public final class TCB {
	/**
	 * Allocate a new TCB.
	 */
	public TCB() {
	}

	/**
	 * Give the TCB class the necessary privilege to create threads. This is
	 * necessary, because unlike other machine classes that need privilege, we
	 * want the kernel to be able to create TCB objects on its own.
	 * 
	 * @param privilege
	 *            encapsulates privileged access to the Nachos machine.
	 */
	public static void givePrivilege(Privilege privilege) {
		TCB.privilege = privilege;
		privilege.tcb = new TCBPrivilege();
	}

	/**
	 * Causes the thread represented by this TCB to begin execution. The
	 * specified target is run in the thread.
	 */
	public void start(Runnable target) {
		// make sure this TCB has not already been started
		Lib.assert_(javaThread == null && !done);
		// make sure there aren't too many threads already
		Lib.assert_(runningThreads.size() < maxThreads);

		isFirstTCB = runningThreads.isEmpty();

		if (!isFirstTCB) {
			// make sure the current TCB is correct
			Lib.assert_(currentTCB != null
					&& currentTCB.javaThread == Thread.currentThread());
		}

		// TCB start has been approved. add to collection of running threads.
		runningThreads.add(this);

		this.target = target;

		// if not the first, have to make a thread to run
		if (!isFirstTCB) {
			tcbTarget = new Runnable() {
				public void run() {
					threadroot();
				}
			};

			// creating threads is a privileged operation
			privilege.doPrivileged(new Runnable() {
				public void run() {
					javaThread = new Thread(tcbTarget);
				}
			});

			// now start thread and wait for it to notify us from threadroot
			currentTCB.running = false;

			this.javaThread.start();
			currentTCB.waitForInterrupt();
		}
		// otherwise, just call threadroot directly...
		else {
			currentTCB = this;

			javaThread = Thread.currentThread();

			threadroot();
		}
	}

	/**
	 * Return the TCB of the currently running thread.
	 */
	public static TCB currentTCB() {
		return currentTCB;
	}

	/**
	 * Context switch between the current TCB and this TCB. This TCB will become
	 * the new current TCB. It is acceptable for this TCB to be the current TCB.
	 */
	public void contextSwitch() {
		// make sure the current TCB is correct
		Lib.assert_(currentTCB != null
				&& currentTCB.javaThread == Thread.currentThread());

		// make sure AutoGrader.runningThread() called associateThread()
		Lib.assert_(currentTCB.associated);
		currentTCB.associated = false;

		// can't switch from a TCB to itself
		if (this == currentTCB)
			return;

		/*
		 * There are some synchronization concerns here. As soon as we wake up
		 * the next thread, we cannot assume anything about static variables, or
		 * about any TCB's state. Therefore, before waking up the next thread,
		 * we must latch the value of currentTCB, and set its running flag to
		 * false (so that, in case we get interrupted before we call yield(),
		 * the interrupt will set the running flag and yield() won't block).
		 */

		TCB previous = currentTCB;
		previous.running = false;

		this.interrupt();
		previous.yield();
	}

	/**
	 * Destroy this TCB. This TCB must not be in use by the current thread. This
	 * TCB must also have been authorized to be destroyed by the autograder.
	 */
	public void destroy() {
		// make sure the current TCB is correct
		Lib.assert_(currentTCB != null
				&& currentTCB.javaThread == Thread.currentThread());
		// can't destroy current thread
		Lib.assert_(this != currentTCB);
		// thread must have started but not be destroyed yet
		Lib.assert_(javaThread != null && !done);

		// ensure AutoGrader.finishingCurrentThread() called authorizeDestroy()
		Lib.assert_(nachosThread == toBeDestroyed);
		toBeDestroyed = null;

		this.done = true;
		currentTCB.running = false;

		this.interrupt();
		currentTCB.waitForInterrupt();

		this.javaThread = null;
	}

	/**
	 * Destroy all TCBs and exit Nachos. Same as <tt>Machine.terminate()</tt>.
	 */
	public static void die() {
		privilege.exit(0);
	}

	/**
	 * Test if the current JVM thread belongs to a Nachos TCB. The AWT event
	 * dispatcher is an example of a non-Nachos thread.
	 * 
	 * @return <tt>true</tt> if the current JVM thread is a Nachos thread.
	 */
	public static boolean isNachosThread() {
		return (currentTCB != null && Thread.currentThread() == currentTCB.javaThread);
	}

	private void threadroot() {
		// this should be running the current thread
		Lib.assert_(javaThread == Thread.currentThread());

		if (!isFirstTCB) {
			// make sure currentTCB is some other thread
			Lib.assert_(currentTCB != null && this != currentTCB);

			// let currentTCB get out of start()
			currentTCB.interrupt();

			// wait to be scheduled to run
			this.yield();
		} else {
			// make sure currentTCB is us
			Lib.assert_(currentTCB != null && this == currentTCB);

			// we get to run immediately
			running = true;
		}

		try {
			target.run();

			// no way out of here without going throw one of the catch blocks
			Lib.assertNotReached();
		} catch (ThreadDeath e) {
			// make sure this TCB is being destroyed properly
			if (!done) {
				System.err.print("\nTCB terminated improperly!\n");
				privilege.exit(1);
			}

			runningThreads.removeElement(this);
			if (runningThreads.isEmpty())
				privilege.exit(0);
		} catch (Throwable e) {
			e.printStackTrace();

			runningThreads.removeElement(this);
			if (runningThreads.isEmpty())
				privilege.exit(1);
			else
				die();
		}
	}

	private void yield() {
		waitForInterrupt();

		if (done) {
			currentTCB.interrupt();
			throw new ThreadDeath();
		}

		currentTCB = this;
	}

	private synchronized void waitForInterrupt() {
		while (!running) {
			try {
				wait();
			} catch (InterruptedException e) {
			}

			if (currentTCB == null)
				throw new ThreadDeath();
		}
	}

	private synchronized void interrupt() {
		running = true;
		notify();
	}

	private void associateThread(KThread thread) {
		// make sure AutoGrader.runningThread() gets called only once per
		// context switch
		Lib.assert_(!associated);
		associated = true;

		Lib.assert_(thread != null);

		if (nachosThread != null)
			Lib.assert_(thread == nachosThread);
		else
			nachosThread = thread;
	}

	private static void authorizeDestroy(KThread thread) {
		// make sure AutoGrader.finishingThread() gets called only once per
		// destroy
		Lib.assert_(toBeDestroyed == null);
		toBeDestroyed = thread;
	}

	/**
	 * The maximum number of started, non-destroyed TCB's that can be in
	 * existence.
	 */
	public static final int maxThreads = 250;

	private static TCB currentTCB = null;
	private static int numThreads = 0;
	private static Privilege privilege;
	private static KThread toBeDestroyed = null;
	private static Vector runningThreads = new Vector();

	private Thread javaThread = null;
	private KThread nachosThread = null;
	private boolean associated = false;
	private boolean running = false;
	private boolean done = false;
	private Runnable target;
	private Runnable tcbTarget;
	private boolean isFirstTCB;

	private static class TCBPrivilege implements Privilege.TCBPrivilege {
		public void associateThread(KThread thread) {
			Lib.assert_(currentTCB != null);
			currentTCB.associateThread(thread);
		}

		public void authorizeDestroy(KThread thread) {
			TCB.authorizeDestroy(thread);
		}
	}
}
