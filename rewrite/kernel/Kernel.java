package kernel;

import java.io.IOException;

import coff.CoffLoadException;
import coff.Loader;

import hardware.Interrupt;
import hardware.Timer;
import emulator.Memory;
import emulator.MipsException;
import emulator.Processor;
import filesystem.FileTableEntry;
import filesystem.FileSystem;

import machine.Configuration;
import machine.Lib;
import machine.Page;
import machine.Machine;
import static emulator.MipsException.*;

/**
 * Main class - handles interrupts, system calls, scheduling and memory management.
 * 
 * @author pauljohnson
 *
 */
public class Kernel implements Runnable{
	// list of system processes
	public PCB[] processes = new PCB[Configuration.maxProcesses];
	
	// current running process
	public PCB process;
	
	// has the kernel been initialized?
	private boolean initialized = false;
	
	// the process scheduler
	private Scheduler scheduler;
	
	// the algorithm to use for page replacement
	private PageReplacement pageReplacer;
	
	// the filesystem
	private FileSystem fs;
	
	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
	syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
	syscallRead = 6, syscallWrite = 7, syscallClose = 8,
	syscallUnlink = 9, syscallKernelInit = 13;
	
	private Machine machine;
	
	public Kernel(Machine machine){
		this.machine = machine;
	}
	
	/**
	 * This doesn't have anything to do with threading - its just used by the process to 
	 * handle interrupts and exceptions
	 */
	public void run() {
		Processor processor = machine.processor();
		int cause = processor.readRegister(Processor.regCause);
		
		switch(cause){		
		case exceptionSyscall:
			syscall();
			
			machine.processor().advancePC();
			break;
		case exceptionPageFault:
			pageFault();
			
			//machine.processor().advancePC();
			break;
		case exceptionTLBMiss:
			tlbMiss();
			break;
		case exceptionReadOnly:
		case exceptionBusError:
		case exceptionAddressError:
		case exceptionOverflow:
		case exceptionIllegalInstruction:
			exception();
			break;
		case exceptionInterrupt:
			interrupt();
			
			if(process.ticks > Configuration.quantum){
				//machine.processor().advancePC();
			}
			break;
		default:
			throw new KernelFault("Unknown Processor Exception");
		}
		
		// schedule the next process to run
		schedule();

	}
	
	/**
	 * Schedules the next process if necessary
	 * 
	 */
	public void schedule(){
		// do nothing
		if(process != null && process.ticks < Configuration.quantum && process.state == PCB.ready){
			return;
		}
		
		PCB nextProcess = this.scheduler.schedule();
		
		if(nextProcess == null){
			throw new KernelFault("No Processes available for scheduling");
		}
		
		// save current process
		if(process != null){
			process.ticks = 0;
			
			for (int i = 0; i < Processor.numUserRegisters; i++){
				process.userRegisters[i] = machine.processor().readRegister(i);
			}
			
			// save dirty pages to page table
			for(int i = 0; i < process.pageTable.length; i++){
				Page entry = process.pageTable[i];
				
				if(entry != null && entry.present && entry.dirty){
					for(int j = 0; j < Configuration.pageSize; j++){
						try {
							entry.data[j] = (byte)machine.memory().readMem(Memory.makeAddress(entry.vpn, j), 1);
						} catch (MipsException e) {
						}
					}
				}
			}
		}
		
		// restore next process
		for (int i = 0; i < Processor.numUserRegisters; i++){
			machine.processor().writeRegister(i, nextProcess.userRegisters[i]);
		}
		

		
		machine.memory().setPageTable(nextProcess.pageTable);
		
		
		process = nextProcess;
		
		System.out.println(process.name + " On Processor");
	}
	
	/**
	 * Handle a processor exception
	 */
	public void exception(){
		throw new KernelFault("Exception in Processor");
	}

	/**
	 * Handle a processor interrupt
	 */
	public void interrupt(){
		Interrupt interrupt = machine.interrupting;
		
		if(interrupt instanceof Timer){
			process.ticks++;
		}
		
		machine.interrupting = null;
		interrupt.acknowledge();
	}
	
	/**
	 * Handle a page fault
	 */
	public void pageFault(){
		// get page that needs to be inserted
		int badVaddr = machine.processor().readRegister(Processor.regBadVAddr);
		
		int virtualPageNumber = Memory.pageFromAddress(badVaddr);
		
		//System.out.println("Virtual Page Number: " + virtualPageNumber);
		
		Page virtualPage = process.pageTable[virtualPageNumber];
		
		// get physical page number to put virtual page in
		//int physicalPageNumber = pageReplacer.getPage();
		pageReplacer.replace();
		Page replacedPage = pageReplacer.getReplacedPage();
		
		if(replacedPage != null){
			replacedPage.ppn = -1;
			replacedPage.present = false;
			replacedPage.readOnly = false;
			replacedPage.used = false;
			
			if(replacedPage.dirty){
				// evicting page set dirty to copy data from memory to virtual page
				System.arraycopy(machine.memory().mainMemory, replacedPage.ppn * Configuration.pageSize, replacedPage, 0, Configuration.pageSize);
			}
			
			replacedPage.dirty = false;
		}
		
		int physicalPageNumber = pageReplacer.getPhysicalPageNumber();
		
		// set details in page table entry
		virtualPage.ppn = physicalPageNumber;
		virtualPage.pid = process.pid;
		virtualPage.present = true;
		virtualPage.readOnly = false;
		virtualPage.used = false;
		virtualPage.dirty = false;
		
		System.arraycopy(virtualPage.data, 0, machine.memory().mainMemory, physicalPageNumber * Configuration.pageSize, Configuration.pageSize);
	}
	
	/** 
	 * Handle TLB miss - not implemented
	 */
	public void tlbMiss(){
		
	}
	
	/**
	 * Handle a syscall from a program
	 * 
	 */
	public void syscall(){
		
		Processor processor = machine.processor();
		
		int syscall = processor.readRegister(Processor.regV0);
		
		switch(syscall){
		case syscallHalt:
			System.out.println("Syscall Halt");
			handleHalt();
						
			break;
		case syscallExit:
			System.out.println("Syscall Exit");
			handleExit();
			break;
		case syscallExec:
			System.out.println("Syscall Exec");
			handleExec();
			
			break;
		case syscallJoin:
			System.out.println("Syscall Join");
			handleJoin();
			break;
		case syscallCreate:
			System.out.println("Syscall Create");
			handleCreate();
			break;
		case syscallOpen:
			System.out.println("Syscall Open");
			handleOpen();
			break;
		case syscallRead:
			//System.out.println("Syscall Read");
			handleRead();
			break; 
		case syscallWrite:
			//System.out.println("Syscall Write");
			handleWrite();
			break; 
		case syscallClose:
			System.out.println("Syscall Close");
			handleClose();
			break;
		case syscallUnlink:
			System.out.println("Syscall Unlink");
			handleUnlink();

			break; 
		case syscallKernelInit:
			if(!initialized){
				initialize();
			}else{
				throw new KernelFault("Kernel already initialized");
			}
			break;
		default:
			throw new KernelFault("Unknown Syscall");
		}
	}
	
	/**
	 * Syscall handler methods
	 */

	private void handleUnlink() {
		String name = getStringFromMemory(Processor.regA0);
		
		int rval = fs.unlink(name);
		
		machine.processor().writeRegister(Processor.regV0, rval);
	}
	
	private void handleExec() {
		int namePointer = machine.processor().readRegister(Processor.regA0);
		
		String name = getStringFromMemoryPointer(namePointer);
		
		int argc = machine.processor().readRegister(Processor.regA1);
		
		int argsPointer = machine.processor().readRegister(Processor.regA2);
		
		checkInMemory(argsPointer);
		
		// read args from memory
		String[] args = new String[argc];
		
		for(int i = 0; i < argc; i++){
			try {
				int argPointer = machine.memory().readMem(argsPointer+(i*4), 4);
				
				checkInMemory(argPointer);
				
				args[i] = getStringFromMemoryPointer(argPointer);
			} catch (MipsException e) {
				throw new KernelFault("bad address");
			}
		}
		
		handleExec(name, args);
	}
	
	/**
	 * Lazy version of exec for use by initialize
	 * 
	 * @param name
	 * @param args
	 */
	private void handleExec(String name, String[] args){
		PCB new_process = new PCB();
		
		new_process.name = name;
		
		new_process.pageTable = new Page[Configuration.numPhysPages];
		
		addProcess(new_process);
		
		if(process == null){
			new_process.parent = -1;
		}else{
			new_process.parent = process.pid;
		}
		

		int fid = fs.open(name, new_process);
		
		if(fid == -1){
			throw new KernelFault("Shell program not found");
		}
		
		// swap the new processes page table into memory so we can read data into it
		machine.memory().setPageTable(new_process.pageTable);
		PCB old_process = process;
		process = new_process;

		// add a couple of pages to memory to hold file headers while loading
		new_process.pageTable[0] = new Page(0, -1, false, false, false, false);
		new_process.pageTable[1] = new Page(1, -1, false, false, false, false);		
		try {
			Loader loader = new Loader();
				
			loader.load(fid, new_process, fs, this, machine.memory(), args);
			
			// by default, everything's 0
			for (int i = 0; i < Processor.numUserRegisters; i++)
				new_process.userRegisters[i] = 0;

			// initialize PC and SP according
			new_process.userRegisters[Processor.regPC] = loader.programCounter;
			new_process.userRegisters[Processor.regSP] = loader.stackPointer;
			
			new_process.userRegisters[Processor.regNextPC] = new_process.userRegisters[Processor.regPC] + 4;
			
			// initialize the first two argument registers to argc and argv
			new_process.userRegisters[Processor.regA0] = loader.argc;
			new_process.userRegisters[Processor.regA1] = loader.argv;
			
			// reset page table
			
			process = old_process;
			
			if(process != null){
				machine.memory().setPageTable(process.pageTable);
			}
			
			// close file
			fs.close(fid, new_process);
		} catch (CoffLoadException e) {
			throw new KernelFault("Tried to execute file that isn't a coff file");
		} catch (MipsException e) {
			throw new KernelFault("Tried to execute file that isn't a coff file");
		}
	}

	private void handleClose() {
		int rval;
		int fid = machine.processor().readRegister(Processor.regA0);
		
		rval = fs.close(fid, process);
		
		machine.processor().writeRegister(Processor.regV0, rval);
	}

	private void handleWrite() {
		int fid = machine.processor().readRegister(Processor.regA0);
		int startPointer = machine.processor().readRegister(Processor.regA1);
		int length = machine.processor().readRegister(Processor.regA2);
		
		int read = -1;
		
		switch(fid){
		case 1:
			// handle standard out
			
			// make sure processes buffer is in memory
			checkInMemory(startPointer);
			
			// read char from stdin
			read = -1;
			
			try {
				for(int i = 0; i < length; i++){
					int t = machine.memory().readMem(startPointer+i, 1);
				
					//System.out.println(t);
				
					System.out.write((byte)t);
				}
				
				read = length;
			} catch (MipsException e) {
				throw new KernelFault("Bad address");
			}
			break;
		default:
			read = fs.write(fid, length, startPointer, this, process);
		}
		
		machine.processor().writeRegister(Processor.regV0, read);
	}

	private void handleRead() {
		int fid = machine.processor().readRegister(Processor.regA0);
		
		int bufferPointer = machine.processor().readRegister(Processor.regA1);
		int length = machine.processor().readRegister(Processor.regA2);
		
		int read = -1;
		
		switch(fid){
		case 0:
			// handle standard in
			
			// make sure processes buffer is in memory
			checkInMemory(bufferPointer);
			
			// read char from stdin
			read = 1;
			
			try {
				int t = System.in.read();
				
				machine.memory().writeMem(bufferPointer, 1, t);
				
			} catch (IOException e) {
				throw new KernelFault("Unable to read from standard in");
			} catch (MipsException e) {
				throw new KernelFault("Bad address");
			}
		default:
			read = fs.read(fid, length, bufferPointer, this, process);
		}

		machine.processor().writeRegister(Processor.regV0, read);
	}

	private void handleHalt() {
		if(process.pid  == 0){
			machine.halt();
		}else{
			processes[process.pid] = null;
			
			// handle joining processes
			
			// set return value on joining processes
			for(int i = 0; i < Configuration.maxProcesses; i++){
				if(processes[i] != null && processes[i].joining == process.pid){
					PCB joining = processes[i];
					
					joining.state = PCB.ready;
					
					joining.joining = -1;
					joining.statusPointer = 0;
					
					joining.userRegisters[Processor.regV0] = 0;
				}
			}
		}
	}

	private void handleCreate() {
		String name = getStringFromMemory(Processor.regA0);
		
		int rval = fs.create(name, process);
		
		machine.processor().writeRegister(Processor.regV0, rval);
	}

	private void handleOpen() {
		String name = getStringFromMemory(Processor.regA0);
		
		int rval = fs.open(name, process);
		
		machine.processor().writeRegister(Processor.regV0, rval);
	}

	private String getStringFromMemory(int register) {
		int namePointer = machine.processor().readRegister(register);
		
		return getStringFromMemoryPointer(namePointer);
	}
	
	private String getStringFromMemoryPointer(int namePointer){
		Page page = checkInMemory(namePointer);
		
		// page should contain a valid ppn here
		
		byte[] memory = machine.memory().mainMemory;
		
		int start = page.ppn * Configuration.pageSize + Memory.offsetFromAddress(namePointer);
		int length = start;
		byte val;
		
		while((val = memory[length]) != 0){
			length++;
		}
		
		String name = Lib.bytesToString(memory, start, length-start);
		return name;
	}
	
	
	/**
	 * Check if a page for an address is in memory, if it isn't then trigger
	 * a page fault to bring it into memory
	 * 
	 * @param namePointer
	 * @return page that has been placed in memory
	 */
	public Page checkInMemory(int namePointer) {
		if(!machine.memory().vmEnabled){
			return null;
		}
		
		// check if page containing pointer is in memory
		int vpn = Memory.pageFromAddress(namePointer);
		
		//System.out.println("Page Number: " + vpn + " Pointer " + namePointer);
		
		Page page = process.pageTable[vpn];
		
		if(!page.present){
			//System.out.println("Faulting on check");
			machine.processor().writeRegister(Processor.regBadVAddr, namePointer);
			pageFault();
		}
		return page;
	}
	
	private void handleJoin() {
		int childPid = machine.processor().readRegister(Processor.regA0);
		int statusPointer = machine.processor().readRegister(Processor.regA1);
		
		PCB child = processes[childPid];
		
		if(child == null || child.parent != process.pid){
			machine.processor().writeRegister(Processor.regV0, -1);
			return;
		}
		
		process.state = PCB.waiting;
		
		process.joining = child.pid;
		process.statusPointer = statusPointer;
	}

	/**
	 * Called by the boot block to initialize the system
	 */
	public void initialize(){
		this.initialized = true;
		
		this.scheduler = (Scheduler) Lib.constructObject(Configuration.scheduler);
		this.scheduler.setProcesses(processes);
		
		this.pageReplacer = (PageReplacement) Lib.constructObject(Configuration.replacer);
		this.pageReplacer.setProcesses(processes);
		
		// create file system
		this.fs = (FileSystem) Lib.constructObject(Configuration.fileSystem);
		
		this.fs.initialize(machine);
		
		// enable virtual memory
		machine.memory().vmEnabled = true;
		
		// create first process
		handleExec(Configuration.shellProgramName, Configuration.processArgs);
		
		// create idle process
		handleExec("idle.coff", new String[]{});
	
		// start timer to generate context switching interrupts
		new Thread(machine.timer, "Timer Thread").start();
	}

	private void handleExit() {
		System.out.println("Process " + process.name + " exiting");
		
		int status = machine.processor().readRegister(Processor.regA0);
		
		processes[process.pid] = null;
		
		// halt machine if last process has exited
		if(process.pid == 0){
			machine.halt();
		}
		
		// close files
		for(int i = 0; i < Configuration.maxFiles; i++){
			if(process.files[i] != null){
				fs.close(i, process);
			}
		}
		
		// set return value on joining processes
		for(int i = 0; i < Configuration.maxProcesses; i++){
			if(processes[i] != null && processes[i].joining == process.pid){
				PCB joining = processes[i];
				
				joining.state = PCB.ready;
				
				// write return result of process to the joining processes status pointer
				
				// set the current process to joining so memory works correctly
				PCB oldProcess = process;
				process = joining;
				
				machine.memory().setPageTable(joining.pageTable);
				
				Page page = checkInMemory(joining.statusPointer);
				
				int start = page.ppn * Configuration.pageSize + Memory.offsetFromAddress(joining.statusPointer);
				
				System.arraycopy(Lib.bytesFromInt(status), 0, machine.memory().mainMemory, start, 4);
				
				process = oldProcess;
				machine.memory().setPageTable(oldProcess.pageTable);
				
				joining.joining = -1;
				joining.statusPointer = 0;
				
				joining.userRegisters[Processor.regV0] = 1;
			}
		}
		
		// set parents to 0;
		for(int i = 0; i < Configuration.maxProcesses; i++){
			if(processes[i] != null && processes[i].parent == process.pid){
				processes[i].parent = -1;
			}
		}

	}
	
	public int addProcess(PCB pcb){
		for(int i = 0; i < Configuration.maxFiles;i++){
			if(processes[i] == null){
				pcb.pid = i;
				processes[i] = pcb;
				return i;
			}
		}
		
		throw new KernelFault("Too many processes open");
	}
}
