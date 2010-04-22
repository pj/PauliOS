package emulator;

import machine.Lib;

public class MipsException extends Exception {
	/** The names of the CPU exceptions. */
	public static final String exceptionNames[] = { "syscall      ",
			"page fault   ", "TLB miss     ", "read-only    ", "bus error    ",
			"address error", "overflow     ", "illegal inst ", "interrupt " };
	
	/** Caused by a syscall instruction. */
	public static final int exceptionSyscall = 0;
	/** Caused by an access to an invalid virtual page. */
	public static final int exceptionPageFault = 1;
	/** Caused by an access to a virtual page not mapped by any TLB entry. */
	public static final int exceptionTLBMiss = 2;
	/** Caused by a write access to a read-only virtual page. */
	public static final int exceptionReadOnly = 3;
	/** Caused by an access to an invalid physical page. */
	public static final int exceptionBusError = 4;
	/** Caused by an access to a misaligned virtual address. */
	public static final int exceptionAddressError = 5;
	/** Caused by an overflow by a signed operation. */
	public static final int exceptionOverflow = 6;
	/** Caused by an attempt to execute an illegal instruction. */
	public static final int exceptionIllegalInstruction = 7;
	/** Caused by an interrupt **/
	public static final int exceptionInterrupt = 8;
	
	private Processor processor;
	private Memory memory;
	
	public MipsException(Processor processor, Memory memory, int cause) {
		this.cause = cause;
		this.processor = processor;
		this.memory = memory;
	}

	public MipsException(Processor processor, Memory memory, int cause, int badVAddr) {
		this(processor, memory, cause);

		hasBadVAddr = true;
		this.badVAddr = badVAddr;
	}

	public void handle() {
		processor.writeRegister(Processor.regCause, cause);

		if (hasBadVAddr)
			processor.writeRegister(Processor.regBadVAddr, badVAddr);

		memory.finishLoad();

		processor.exceptionHandler.run();
	}

	private boolean hasBadVAddr = false;
	private int cause, badVAddr;
}
