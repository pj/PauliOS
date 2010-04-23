package machine;

import hardware.HardDrive;
import hardware.IOOperation;
import hardware.Interrupt;
import hardware.Timer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import kernel.Kernel;

import emulator.Memory;
import emulator.MipsException;
import emulator.Processor;

/**
 * Represents the "Computer" on which the OS is running.  Includes the hardware, memory and processor.
 * 
 * @author pauljohnson
 *
 */
public class Machine {
	/** System Kernel */
	Kernel kernel;

	/** The piece of io hardware that is currently interrupting */
	public Interrupt interrupting;

	/** queue of pieces of io hardware that are interrupting and waiting to be handled */
	private PriorityBlockingQueue<Interrupt> interrupts;

	private Processor processor = null;
	private Memory memory = null;

	/** System timer - triggers context switches */
	public Timer timer = null;
	/** Hard drive */
	public HardDrive hd = null;

	private long randomSeed = 0;

	public File baseDirectory, nachosDirectory, testDirectory;
	private String configFileName = "nachos.conf";

	public PriorityBlockingQueue<Interrupt> getInterrupts() {
		return interrupts;
	}

	public Processor processor() {
		return processor;
	}

	public Memory memory() {
		return memory;
	}

	public static void main(final String[] args) throws IOException {
		new Machine().initialize(args);
	}

	/**
	 * Initializes the system hardware, reads the bootblock from the hard disk, creates the kernel (but doesn't initialize it) and starts the emulator.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public void initialize(String[] args) throws IOException {
		baseDirectory = new File(new File("").getAbsolutePath());
		// get the nachos directory (./nachos)
		nachosDirectory = new File(baseDirectory, "nachos");
		// get the test directory (../test)
		testDirectory = new File(baseDirectory.getParentFile(), "test");

		processArgs(args);

		Configuration.processArgs = args;
		
		createDevices();

		// start hard drive device
		new Thread(hd, "Hard Drive thread").start();
		
		// load first block of hard drive
		IOOperation ioop = new IOOperation();
		ioop.action = HardDrive.read;
		ioop.position = 0;
		ioop.length = Configuration.bootBlockLength;
		
		hd.operations.add(ioop);
		try {
			// wait for the hard drive to complete
			interrupts.take().acknowledge();
			
			// ioop will now contain the data from the first block so write it to memory
			for(int i = 0; i < 1024; i++){
				byte b = ioop.rdata[i];
				memory.writeMem(i, 1, b);
			}
			
			// create kernel and set it to catch interrupts from the processor
			kernel = new Kernel(this);
			
			processor.setExceptionHandler(kernel);
			
			// start processor emulating
			processor.emulate();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (MipsException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the various bits of hardware that constitute the system.
	 * 
	 * @throws FileNotFoundException
	 */
	public void createDevices() throws FileNotFoundException {
		interrupts = new PriorityBlockingQueue<Interrupt>();

		memory = new Memory(Configuration.numPhysPages);

		processor = new Processor(this);

		memory.processor = processor;

		processor.memory = memory;

		// create timer
		timer = new Timer(Configuration.switchTime);
		timer.setQueue(interrupts);
		
		// create hardisk
		hd = new HardDrive();
		hd.operations = new LinkedBlockingQueue<IOOperation>();
		hd.setQueue(interrupts);
		
	}
	
	public void startHardware(){
		new Thread(hd).start();
		new Thread(timer).start();
	}

	/**
	 * Print stats, and terminate Nachos.
	 */
	public void halt() {
		System.out.print("Machine halting!\n\n");
		System.exit(0);
	}
	
	private void processArgs(String[] args) {
		for (int i = 0; i < args.length;) {
			String arg = args[i++];
			if (arg.length() > 0 && arg.charAt(0) == '-') {
				if (arg.equals("-h")) {
					printFile("../help.txt");
					System.exit(0);
				} else if (arg.equals("-z")) {
					printFile("../copyright.txt");
					System.exit(0);
				}
			}
		}
		
		Configuration.processArgs = args;

		Lib.seedRandom(randomSeed);
	}

	private void printFile(String name) {
		try {
			File hFile = new File(name);
			
			BufferedReader hFR;
			
			hFR = new BufferedReader(new FileReader(hFile));
			
			String line;
			
			while((line = hFR.readLine()) != null){
				System.out.println(line);
			}
		} catch (FileNotFoundException e) {
			System.out.println(name + " not found");
			System.exit(0);
		} catch (IOException e) {
			System.exit(0);
		}
		

	}

}

