package kernel;

import java.io.File;

import hardware.IOOperation;
import hardware.Interrupt;
import hardware.Timer;
import emulator.Processor;
import filesystem.PassThroughFileSystem;

import machine.Config;
import machine.Lib;
import machine.TranslationEntry;
import machine.Machine;
import static emulator.MipsException.*;

public class Kernel implements Runnable{
	public final static int max_processes = 256;
	public final static int max_files = 256;
	
	// list of 
	private PCB[] processes;
	
	private PCB process;
	
	// page table for the kernel - this will be a one to one mapping
	public TranslationEntry[] pageTable;
	
	private boolean initialized = false;
	
	private Scheduler scheduler;
	
	private OpenFile[] files;
	
	private PassThroughFileSystem fs;
	
	/**
	 * Initialize the kernel running on the machine
	 *
	 * @param machine
	 * @param args arguments to the kernel
	 */
	public void initialize(){
		Machine.processor().setExceptionHandler(this);
		
		processes = new PCB[max_processes];
		files = new OpenFile[max_files];
		
		this.scheduler = (Scheduler) Lib.constructObject(Config.getString("Kernel.scheduler"));
		
		this.scheduler.setProcesses(processes);
		
		this.initialized = true;
	}
	
	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
	syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
	syscallRead = 6, syscallWrite = 7, syscallClose = 8,
	syscallUnlink = 9, syscallKernelInit = 13;
	
	/**
	 * Handle a syscall from a program
	 * 
	 */
	public void syscall(){
		
		Processor processor = Machine.processor();
		
		int syscall = processor.readRegister(Processor.regA0);
		
		switch(syscall){
		case syscallHalt:
			Machine.halt();			
			break;
		case syscallExit:			
			processes[process.pid] = null;
			
			// close files
			for(int i = 0; i < max_files; i++){
				if(files[i] != null && files[i].process == process.pid){
					files[i] = null;
				}
			}
			
			// set return value on joining processes
			for(int i = 0; i < max_processes; i++){
				if(processes[i] != null && processes[i].joining == process.pid){
					PCB joining = processes[i];
					
					joining.state = PCB.ready;
					
					// set return value
				}
			}
			
			// set parents to 0;
			for(int i = 0; i < max_processes; i++){
				if(processes[i] != null && processes[i].parent == process.pid){
					processes[i].parent = -1;
				}
			}
			
			break;
		case syscallExec:
			PCB new_process = new PCB();
			
			new_process.pid = addProcess(new_process);
			
			// load from file system
			
			break;
		case syscallJoin:
			break;
		case syscallCreate:
			break;
		case syscallOpen:
			IOOperation operation = new IOOperation();
			process.currentIO = operation;
			operation.action = PassThroughFileSystem.open;
			// operation.name
			
			// create descriptor
			
			fs.operations.add(operation);
			
			break;
		case syscallRead:
			break; 
		case syscallWrite:
			break; 
		case syscallClose:
			break;
		case syscallUnlink:
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
	 * Handle a processor exception
	 */
	public void exception(){
		throw new KernelFault("Exception in Processor");
	}

	/**
	 * Handle a processor interrupt
	 */
	public void interrupt(){
		Interrupt interrupt = Machine.interrupting;
		
		if(interrupt instanceof Timer){
		}else if(interrupt instanceof PassThroughFileSystem){
			PassThroughFileSystem ptfs = (PassThroughFileSystem)interrupt;
			
			IOOperation operation = ptfs.operation;
			
			// complete IO
			PCB completeProcess = processes[operation.process];
			
			// write into a processes memory
		}
		
		Machine.interrupting = null;
		interrupt.acknowledge();
	}
	
	/**
	 * Handle a page fault
	 */
	public void pageFault(){
		
	}
	
	/** 
	 * Handle TLB miss
	 */
	public void tlbMiss(){
		
	}
	
	/**
	 * Interpret 
	 */
	public void run() {
		Processor processor = Machine.processor();
		int cause = processor.readRegister(Processor.regCause);
		
		switch(cause){		
		case exceptionSyscall:
			syscall();
			break;
		case exceptionPageFault:
			pageFault();
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
			break;
		default:
			throw new KernelFault("Unknown Processor Exception");
		}
		
		// schedule the next process to run
		schedule();
		
		// transfer control back to the emulator;
		Machine.processor().emulate();
	}
	
	public void schedule(){
		PCB nextProcess = this.scheduler.schedule();
		
		// save current process
		for (int i = 0; i < Processor.numUserRegisters; i++){
			process.userRegisters[i] = Machine.processor().readRegister(i);
		}
			
		// restore next process
		for (int i = 0; i < Processor.numUserRegisters; i++){
			Machine.processor().writeRegister(i, nextProcess.userRegisters[i]);
		}
		
		Machine.memory().setPageTable(nextProcess.pageTable);
		
		process = nextProcess;
	}
	
	public int addFile(OpenFile of){
		for(int i = 0; i < max_files;i++){
			if(files[i] == null){
				files[i] = of;
				return i;
			}
		}
		
		throw new KernelFault("Too many files open");
	}
	
	public int addProcess(PCB pcb){
		for(int i = 0; i < max_files;i++){
			if(processes[i] == null){
				processes[i] = pcb;
				return i;
			}
		}
		
		throw new KernelFault("Too many processes open");
	}
}
