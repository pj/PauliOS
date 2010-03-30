// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package machine;

import hardware.Interrupt;
import hardware.Timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.PriorityBlockingQueue;

import coff.CoffLoadException;
import coff.CoffLoader;
import coff.CoffStartData;

import kernel.Kernel;

import emulator.Memory;
import emulator.Processor;

/**
 * The master class of the simulated machine. Processes command line arguments,
 * constructs all simulated hardware devices, and starts the grader.
 */
public final class Machine {
	/**
	 * Nachos main entry point.
	 * 
	 * @param args
	 *            the command line arguments.
	 */
	public static void main(final String[] args) {
		System.out.print("nachos 5.0j initializing...");

		Lib.assert_(Machine.args == null);
		Machine.args = args;

		// get the current directory (.)
		baseDirectory = new File(new File("").getAbsolutePath());
		// get the nachos directory (./nachos)
		nachosDirectory = new File(baseDirectory, "nachos");
		// get the test directory (../test)
		testDirectory = new File(baseDirectory.getParentFile(), "test");

		processArgs();

		Config.load(configFileName);

		createDevices();
		
		// create kernel
		kernel = new Kernel();
		
		// load core os here
		CoffLoader loader = new CoffLoader();
		
		FileInputStream fis;
		try {
			fis = new FileInputStream(new File(Config.getString("Machine.coreos")));
			
			CoffStartData csd = loader.load(fis, args, kernel.pageTable);
			
			memory.setPageTable(kernel.pageTable);
			
			// by default, everything's 0
			for (int i = 0; i < processor.numUserRegisters; i++)
				processor.writeRegister(i, 0);

			// initialize PC and SP according
			processor.writeRegister(Processor.regPC, csd.initialPC);
			processor.writeRegister(Processor.regSP, csd.initialSP);

			// initialize the first two argument registers to argc and argv
			processor.writeRegister(Processor.regA0, csd.argc);
			processor.writeRegister(Processor.regA1, csd.argv);
			
			// start processor in core os
			processor.emulate();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoffLoadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	static Kernel kernel;

	/**
	 * Terminate Nachos. Same as <tt>TCB.die()</tt>.
	 */
	public static void terminate() {
		System.exit(0);
	}

	/**
	 * Terminate Nachos as the result of an unhandled exception or error.
	 * 
	 * @param e
	 *            the exception or error.
	 */
	public static void terminate(Throwable e) {
		if (e instanceof ThreadDeath)
			throw (ThreadDeath) e;

		e.printStackTrace();
		terminate();
	}

	/**
	 * Print stats, and terminate Nachos.
	 */
	public static void halt() {
		System.out.print("Machine halting!\n\n");
		terminate();
	}

	private static void processArgs() {
		for (int i = 0; i < args.length;) {
			String arg = args[i++];
			if (arg.length() > 0 && arg.charAt(0) == '-') {
				if (arg.equals("-d")) {
					Lib.assert_(i < args.length);
					Lib.enableDebugFlags(args[i++]);
				} else if (arg.equals("-h")) {
					System.out.print(help);
					System.exit(1);
				} else if (arg.equals("-s")) {
					Lib.assert_(i < args.length);
					try {
						randomSeed = Long.parseLong(args[i++]);
					} catch (NumberFormatException e) {
						Lib.assertNotReached();
					}
				} else if (arg.equals("-x")) {
					Lib.assert_(i < args.length);
					shellProgramName = args[i++];
				} else if (arg.equals("-z")) {
					System.out.print(copyright);
					System.exit(1);
				}
				// these switches are reserved for the autograder
				else if (arg.equals("-[]")) {
					Lib.assert_(i < args.length);
					configFileName = args[i++];
				} else if (arg.equals("--")) {
					Lib.assert_(i < args.length);
				}
			}
		}

		Lib.seedRandom(randomSeed);
	}
	
	private static PriorityBlockingQueue<Interrupt> interrupts;
	
	public static PriorityBlockingQueue<Interrupt> getInterrupts() {
		return interrupts;
	}
	
	public static Interrupt interrupting;

	private static void createDevices() {
		int numPhysPages = Config.getInteger("Processor.numPhysPages");
		memory = new Memory(processor, numPhysPages);
		
		processor = new Processor(memory);
		
		// create timer
		timer = new Timer(Config.getInteger("Machine.switch_time"));
	}


	/**
	 * Prevent instantiation.
	 */
	private Machine() {
	}

	/**
	 * Return the MIPS processor.
	 * 
	 * @return the MIPS processor, or <tt>null</tt> if it is not present.
	 */
	public static Processor processor() {
		return processor;
	}
	
	/**
	 * Return the MIPS processor.
	 * 
	 * @return the MIPS processor, or <tt>null</tt> if it is not present.
	 */
	public static Memory memory() {
		return memory;
	}

	private static Processor processor = null;
	private static Memory memory = null;
	private static Timer timer = null;
	
	/**
	 * Return the name of the shell program that a user-programming kernel must
	 * run. Make sure <tt>UserKernel.run()</tt> <i>always</i> uses this method
	 * to decide which program to run.
	 * 
	 * @return the name of the shell program to run.
	 */
	public static String getShellProgramName() {
		if (shellProgramName == null)
			shellProgramName = Config.getString("Kernel.shellProgram");

		Lib.assert_(shellProgramName != null);
		return shellProgramName;
	}

	private static String shellProgramName = null;

	/**
	 * Return the name of the process class that the kernel should use. In the
	 * multi-programming project, returns <tt>nachos.userprog.UserProcess</tt>.
	 * In the VM project, returns <tt>nachos.vm.VMProcess</tt>. In the
	 * networking project, returns <tt>nachos.network.NetProcess</tt>.
	 * 
	 * @return the name of the process class that the kernel should use.
	 * 
	 * @see nachos.userprog.UserKernel#run
	 * @see nachos.userprog.UserProcess
	 * @see nachos.vm.VMProcess
	 * @see nachos.network.NetProcess
	 */
	public static String getProcessClassName() {
		if (processClassName == null)
			processClassName = Config.getString("Kernel.processClassName");

		Lib.assert_(processClassName != null);
		return processClassName;
	}

	private static String coreOS;
	
	private static String processClassName = null;
	
	private static String[] args = null;

	private static long randomSeed = 0;

	private static File baseDirectory, nachosDirectory, testDirectory;
	private static String configFileName = "nachos.conf";

	private static final String help = "\n"
			+ "Options:\n"
			+ "\n"
			+ "\t-d <debug flags>\n"
			+ "\t\tEnable some debug flags, e.g. -d ti\n"
			+ "\n"
			+ "\t-h\n"
			+ "\t\tPrint this help message.\n"
			+ "\n"
			+ "\t-s <seed>\n"
			+ "\t\tSpecify the seed for the random number generator (seed is a\n"
			+ "\t\tlong).\n" + "\n" + "\t-x <program>\n"
			+ "\t\tSpecify a program that UserKernel.run() should execute,\n"
			+ "\t\tinstead of the value of the configuration variable\n"
			+ "\t\tKernel.shellProgram\n" + "\n" + "\t-z\n"
			+ "\t\tprint the copyright message\n" + "\n"
			+ "\t-- <grader class>\n"
			+ "\t\tSpecify an autograder class to use, instead of\n"
			+ "\t\tnachos.ag.AutoGrader\n" + "\n" + "\t-# <grader arguments>\n"
			+ "\t\tSpecify the argument string to pass to the autograder.\n"
			+ "\n" + "\t-[] <config file>\n"
			+ "\t\tSpecifiy a config file to use, instead of nachos.conf\n"
			+ "";

	private static final String copyright = "\n"
			+ "Copyright 1992-2001 The Regents of the University of California.\n"
			+ "All rights reserved.\n"
			+ "\n"
			+ "Permission to use, copy, modify, and distribute this software and\n"
			+ "its documentation for any purpose, without fee, and without\n"
			+ "written agreement is hereby granted, provided that the above\n"
			+ "copyright notice and the following two paragraphs appear in all\n"
			+ "copies of this software.\n"
			+ "\n"
			+ "IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY\n"
			+ "PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL\n"
			+ "DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS\n"
			+ "DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN\n"
			+ "ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
			+ "\n"
			+ "THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY\n"
			+ "WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n"
			+ "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE\n"
			+ "SOFTWARE PROVIDED HEREUNDER IS ON AN \"AS IS\" BASIS, AND THE\n"
			+ "UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE\n"
			+ "MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.\n";
}
