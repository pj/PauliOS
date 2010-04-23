package kernel;

import hardware.IOOperation;
import machine.Configuration;
import machine.Machine;
import machine.Page;
import emulator.Processor;
import filesystem.FileTableEntry;
import filesystem.OpenFile;

/**
 * Process Control Block. Contains information about a process.
 * @author pauljohnson
 *
 */
public class PCB {
	
	public static final int ready = 0;
	public static final int waiting = 1;
	public static final int running = 2;
	
	public Page[] pageTable;
	
	public int userRegisters[] = new int[Processor.numUserRegisters];
	
	public IOOperation currentIO;
	
	public int state = 0;
	
	// name of file we are running
	public String name;
	
	public int pid;
	
	public int parent;
	
	// pid of child we are joining
	public int joining = -1;
	
	// pointer where to put status of exited child process
	public int statusPointer = 0;
	
	// number of ticks this process has been on the processor
	public int ticks;
	
	/**
	 * This processes IO Operation
	 */	
	public IOOperation getCurrentIO() {
		return currentIO;
	}

	public void setCurrentIO(IOOperation currentIO) {
		this.currentIO = currentIO;
	}


	// files
	public OpenFile[] files = new OpenFile[Configuration.maxFiles];

}
