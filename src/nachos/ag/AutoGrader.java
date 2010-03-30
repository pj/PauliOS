// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.ag;

import machine.Config;
import machine.Lib;
import nachos.machine.*;
import nachos.security.*;
import nachos.threads.*;

import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * The default autograder. Loads the kernel, and then tests it using
 * <tt>Kernel.selfTest()</tt>.
 */
public class AutoGrader {
	/**
	 * Allocate a new autograder.
	 */
	public AutoGrader() {
	}

	/**
	 * Start this autograder. Extract the <tt>-#</tt> arguments, call
	 * <tt>init()</tt>, load and initialize the kernel, and call <tt>run()</tt>.
	 * 
	 * @param privilege
	 *            encapsulates privileged access to the Nachos machine.
	 * @param args
	 *            the command line arguments to Nachos.
	 */
	public void start(Privilege privilege, String[] args) {
		Lib.assert_(this.privilege == null);
		this.privilege = privilege;

		extractArguments(args);

		System.out.print(" grader");

		init();

		System.out.print("\n");

		kernel = (Kernel) Lib
				.constructObject(Config.getString("Kernel.kernel"));
		kernel.initialize(args);

		run();
	}

	private void extractArguments(String[] args) {
		String testArgsString = "";

		for (int i = 0; i < args.length;) {
			String arg = args[i++];
			if (arg.length() > 0 && arg.charAt(0) == '-') {
				if (arg.equals("-#")) {
					Lib.assert_(i < args.length);
					testArgsString = args[i++];
				}
			}
		}

		StringTokenizer st = new StringTokenizer(testArgsString);

		while (st.hasMoreTokens()) {
			StringTokenizer pair = new StringTokenizer(st.nextToken(), "=");

			Lib.assert_(pair.hasMoreTokens());
			String key = pair.nextToken();

			Lib.assert_(pair.hasMoreTokens());
			String value = pair.nextToken();

			testArgs.put(key, value);
		}
	}

	String getStringArgument(String key) {
		String value = (String) testArgs.get(key);
		Lib.assert_(value != null);
		return value;
	}

	int getIntegerArgument(String key) {
		try {
			return Integer.parseInt(getStringArgument(key));
		} catch (NumberFormatException e) {
			Lib.assertNotReached();
			return 0;
		}
	}

	boolean getBooleanArgument(String key) {
		String value = getStringArgument(key);

		if (value.equals("1") || value.toLowerCase().equals("true")) {
			return true;
		} else if (value.equals("0") || value.toLowerCase().equals("false")) {
			return false;
		} else {
			Lib.assertNotReached();
			return false;
		}
	}

	long getTime() {
		return privilege.stats.totalTicks;
	}

	void targetLevel(int targetLevel) {
		this.targetLevel = targetLevel;
	}

	void level(int level) {
		this.level++;
		Lib.assert_(level == this.level);

		if (level == targetLevel)
			done();
	}

	private int level = 0, targetLevel = 0;

	void done() {
		System.out.print("\nsuccess\n");
		privilege.exit(162);
	}

	private Hashtable testArgs = new Hashtable();

	void init() {
	}

	void run() {
		kernel.selfTest();
		kernel.run();
		kernel.terminate();
	}

	Privilege privilege = null;
	Kernel kernel;

	/**
	 * Notify the autograder that the specified thread is the idle thread.
	 * <tt>KThread.createIdleThread()</tt> <i>must</i> call this method before
	 * forking the idle thread.
	 * 
	 * @param idleThread
	 *            the idle thread.
	 */
	public void setIdleThread(KThread idleThread) {
	}

	/**
	 * Notify the autograder that the specified thread has moved to the ready
	 * state. <tt>KThread.ready()</tt> <i>must</i> call this method before
	 * returning.
	 * 
	 * @param thread
	 *            the thread that has been added to the ready set.
	 */
	public void readyThread(KThread thread) {
	}

	/**
	 * Notify the autograder that the specified thread is now running.
	 * <tt>KThread.restoreState()</tt> <i>must</i> call this method before
	 * returning.
	 * 
	 * @param thread
	 *            the thread that is now running.
	 */
	public void runningThread(KThread thread) {
		privilege.tcb.associateThread(thread);
		currentThread = thread;
	}

	/**
	 * Notify the autograder that the current thread has finished.
	 * <tt>KThread.finish()</tt> <i>must</i> call this method before putting the
	 * thread to sleep and scheduling its TCB to be destroyed.
	 */
	public void finishingCurrentThread() {
		privilege.tcb.authorizeDestroy(currentThread);
	}

	/**
	 * Notify the autograder that a timer interrupt occurred and was handled by
	 * software if a timer interrupt handler was installed. Called by the
	 * hardware timer.
	 * 
	 * @param privilege
	 *            proves the authenticity of this call.
	 * @param time
	 *            the actual time at which the timer interrupt was issued.
	 */
	public void timerInterrupt(Privilege privilege, long time) {
		Lib.assert_(privilege == this.privilege);
	}

	/**
	 * Notify the autograder that a user program executed a syscall instruction
	 * was executed.
	 * 
	 * @param privilege
	 *            proves the authenticity of this call.
	 * @return <tt>true</tt> if the kernel exception handler should be called.
	 */
	public boolean exceptionHandler(Privilege privilege) {
		assert (privilege == this.privilege);
		return true;
	}

	private KThread currentThread;
}
