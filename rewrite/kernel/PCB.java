package kernel;

import hardware.IOOperation;
import machine.Machine;
import machine.TranslationEntry;
import emulator.Processor;

public class PCB {
	
	public static final int ready = 0;
	public static final int waiting = 1;
	public static final int running = 2;
	
	public TranslationEntry[] pageTable;
	
	public int userRegisters[] = new int[Processor.numUserRegisters];
	
	public IOOperation currentIO;
	
	public int state = 0;
	
	// name of file we are running
	public String name;
	
	public int pid;
	
	public int parent;
	
	public int joining;
	
	/**
	 * This processes IO Operation
	 */	
	public IOOperation getCurrentIO() {
		return currentIO;
	}

	public void setCurrentIO(IOOperation currentIO) {
		this.currentIO = currentIO;
	}




}
