// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package emulator;

import hardware.Interrupt;
import machine.Machine;
import machine.Lib;
import machine.Page;

/**
 * The <tt>Processor</tt> class simulates a MIPS processor that supports a
 * subset of the R3000 instruction set. Specifically, the processor lacks all
 * coprocessor support, and can only execute in user mode. Address translation
 * information is accessed via the API. The API also allows a kernel to set an
 * exception handler to be called on any user mode exception.
 * 
 * <p>
 * The <tt>Processor</tt> API is re-entrant, so a single simulated processor can
 * be shared by multiple user threads.
 * 
 * <p>
 * An instance of a <tt>Processor</tt> also includes pages of physical memory
 * accessible to user programs, the size of which is fixed by the constructor.
 */
public final class Processor {
	// system memory
	public Memory memory;
	
	private Machine machine;
	
	
	/**
	 * Allocate a new MIPS processor, with the specified amount of memory.
	 * 
	 * @param privilege
	 *            encapsulates privileged access to the Nachos machine.
	 * @param numPhysPages
	 *            the number of pages of physical memory to attach.
	 */
	public Processor(Machine machine) {
		this.machine = machine;
		
		for (int i = 0; i < numUserRegisters; i++)
			registers[i] = 0;
	}

	/**
	 * Set the exception handler, called whenever a user exception occurs.
	 * 
	 * <p>
	 * When the exception handler is called, interrupts will be enabled, and the
	 * CPU cause register will specify the cause of the exception (see the
	 * <tt>exception<i>*</i></tt> constants).
	 * 
	 * @param exceptionHandler
	 *            the kernel exception handler.
	 */
	public void setExceptionHandler(Runnable exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Start executing instructions at the current PC. Never returns.
	 */
	public void emulate() {
		Lib.debug(dbgProcessor, "starting program in current thread");

		registers[regNextPC] = registers[regPC] + 4;

		while (true) {
			try {
				run();
			} catch (MipsException e) {
				e.handle();
			}			
		}
	}

	/**
	 * Read and return the contents of the specified CPU register.
	 * 
	 * @param number
	 *            the register to read.
	 * @return the value of the register.
	 */
	public int readRegister(int number) {
		Lib.assert_(number >= 0 && number < numUserRegisters);

		return registers[number];
	}

	/**
	 * Write the specified value into the specified CPU register.
	 * 
	 * @param number
	 *            the register to write.
	 * @param value
	 *            the value to write.
	 */
	public void writeRegister(int number, int value) {
		if (number != 0)
			registers[number] = value;
	}

	/**
	 * Advance the PC to the next instruction.
	 * 
	 * <p>
	 * Transfer the contents of the nextPC register into the PC register, and
	 * then add 4 to the value in the nextPC register. Same as
	 * <tt>advancePC(readRegister(regNextPC)+4)</tt>.
	 * 
	 * <p>
	 * Use after handling a syscall exception so that the processor will move on
	 * to the next instruction.
	 */
	public void advancePC() {
		advancePC(registers[regNextPC] + 4);
	}

	/**
	 * Transfer the contents of the nextPC register into the PC register, and
	 * then write the nextPC register.
	 * 
	 * @param nextPC
	 *            the new value of the nextPC register.
	 */
	private void advancePC(int nextPC) {
		registers[regPC] = registers[regNextPC];
		registers[regNextPC] = nextPC;
	}

	/** Index of return value register 0. */
	public static final int regV0 = 2;
	/** Index of return value register 1. */
	public static final int regV1 = 3;
	/** Index of argument register 0. */
	public static final int regA0 = 4;
	/** Index of argument register 1. */
	public static final int regA1 = 5;
	/** Index of argument register 2. */
	public static final int regA2 = 6;
	/** Index of argument register 3. */
	public static final int regA3 = 7;
	/** Index of the stack pointer register. */
	public static final int regSP = 29;
	/** Index of the return address register. */
	public static final int regRA = 31;
	/** Index of the low register, used for multiplication and division. */
	public static final int regLo = 32;
	/** Index of the high register, used for multiplication and division. */
	public static final int regHi = 33;
	/** Index of the program counter register. */
	public static final int regPC = 34;
	/** Index of the next program counter register. */
	public static final int regNextPC = 35;
	/** Index of the exception cause register. */
	public static final int regCause = 36;
	/** Index of the exception bad virtual address register. */
	public static final int regBadVAddr = 37;

	/** The total number of software-accessible CPU registers. */
	public static final int numUserRegisters = 38;

	/** MIPS registers accessible to the kernel. */
	int registers[] = new int[numUserRegisters];


	/** The kernel exception handler, called on every user exception. */
	Runnable exceptionHandler = null;

	public static final char dbgProcessor = 'p';
	public static final char dbgDisassemble = 'm';
	public static final char dbgFullDisassemble = 'M';

	public void run() throws MipsException {
		// hopefully this looks familiar to 152 students?
		fetch();
		decode();
		execute();
		writeBack();

		// check interrupts here
		Interrupt interrupt = machine.getInterrupts().poll();

		if(interrupt != null){
			machine.interrupting = interrupt;
			throw new MipsException(this, memory, MipsException.exceptionInterrupt);
		}
	}

	private boolean test(int flag) {
		return Lib.test(flag, flags);
	}

	private void fetch() throws MipsException {
		
		//System.out.println(this.registers[Processor.regRA]);
		
		//System.out.print("pc: " + Integer.toHexString(registers[regPC]) + " ");
		
		value = memory.readMem(registers[regPC], 4);
	}

	private void decode() {
		op = Lib.extract(value, 26, 6);
		rs = Lib.extract(value, 21, 5);
		rt = Lib.extract(value, 16, 5);
		rd = Lib.extract(value, 11, 5);
		sh = Lib.extract(value, 6, 5);
		func = Lib.extract(value, 0, 6);
		target = Lib.extract(value, 0, 26);
		imm = Lib.extend(value, 0, 16);

		Mips info;
		switch (op) {
		case 0:
			info = Mips.specialtable[func];
			
			break;
		case 1:
			info = Mips.regimmtable[rt];
			break;
		default:
			info = Mips.optable[op];
			break;
		}

		operation = info.operation;
		name = info.name;
		format = info.format;
		flags = info.flags;

		mask = 0xFFFFFFFF;
		branch = true;

		// get memory access size
		if (test(Mips.SIZEB))
			size = 1;
		else if (test(Mips.SIZEH))
			size = 2;
		else if (test(Mips.SIZEW))
			size = 4;
		else
			size = 0;

		// get nextPC
		nextPC = registers[regNextPC] + 4;

		// get dstReg
		if (test(Mips.DSTRA))
			dstReg = regRA;
		else if (format == Mips.IFMT)
			dstReg = rt;
		else if (format == Mips.RFMT)
			dstReg = rd;
		else
			dstReg = -1;

		// get jtarget
		if (format == Mips.RFMT)
			jtarget = registers[rs];
		else if (format == Mips.IFMT)
			jtarget = registers[regNextPC] + (imm << 2);
		else if (format == Mips.JFMT)
			jtarget = (registers[regNextPC] & 0xF0000000) | (target << 2);
		else
			jtarget = -1;

		// get imm
		if (test(Mips.UNSIGNED)) {
			imm &= 0xFFFF;
		}

		// get addr
		addr = registers[rs] + imm;

		// get src1
		if (test(Mips.SRC1SH))
			src1 = sh;
		else
			src1 = registers[rs];

		// get src2
		if (test(Mips.SRC2IMM))
			src2 = imm;
		else
			src2 = registers[rt];

		if (test(Mips.UNSIGNED)) {
			src1 &= 0xFFFFFFFFL;
			src2 &= 0xFFFFFFFFL;
		}

		//System.out.print(Integer.toHexString(value) + "  ");
		//EmulatorHelpers.print(this);
	}

	private void execute() throws MipsException {
		int value;
		int preserved;

		switch (operation) {
		case Mips.ADD:
			dst = src1 + src2;
			break;
		case Mips.SUB:
			dst = src1 - src2;
			break;
		case Mips.MULT:
			dst = src1 * src2;
			registers[regLo] = (int) Lib.extract(dst, 0, 32);
			registers[regHi] = (int) Lib.extract(dst, 32, 32);
			break;
		case Mips.DIV:
			try {
				registers[regLo] = (int) (src1 / src2);
				registers[regHi] = (int) (src1 % src2);
				if (registers[regLo] * src2 + registers[regHi] != src1)
					throw new ArithmeticException();
			} catch (ArithmeticException e) {
				throw new MipsException(this, memory, MipsException.exceptionOverflow);
			}
			break;

		case Mips.SLL:
			dst = src2 << (src1 & 0x1F);
			break;
		case Mips.SRA:
			dst = src2 >> (src1 & 0x1F);
			break;
		case Mips.SRL:
			dst = src2 >>> (src1 & 0x1F);
			break;

			case Mips.SLT:
				dst = (src1 < src2) ? 1 : 0;
				break;

			case Mips.AND:
				dst = src1 & src2;
				break;
			case Mips.OR:
				dst = src1 | src2;
				break;
			case Mips.NOR:
				dst = ~(src1 | src2);
				break;
			case Mips.XOR:
				dst = src1 ^ src2;
				break;
			case Mips.LUI:
				dst = imm << 16;
				break;

			case Mips.BEQ:
				branch = (src1 == src2);
				break;
			case Mips.BNE:
				branch = (src1 != src2);
				break;
			case Mips.BGEZ:
				branch = (src1 >= 0);
				break;
			case Mips.BGTZ:
				branch = (src1 > 0);
				break;
			case Mips.BLEZ:
				branch = (src1 <= 0);
				break;
			case Mips.BLTZ:
				branch = (src1 < 0);
				break;

			case Mips.JUMP:
				break;

			case Mips.MFLO:
				dst = registers[regLo];
				break;
			case Mips.MFHI:
				dst = registers[regHi];
				break;
			case Mips.MTLO:
				registers[regLo] = (int) src1;
				break;
			case Mips.MTHI:
				registers[regHi] = (int) src1;
				break;

			case Mips.SYSCALL:
				throw new MipsException(this, memory, MipsException.exceptionSyscall);

			case Mips.LOAD:
				value = memory.readMem(addr, size);

				if (!test(Mips.UNSIGNED))
					dst = Lib.extend(value, 0, size * 8);
				else
					dst = value;

				break;

			case Mips.LWL:
				value = memory.readMem(addr & ~0x3, 4);

				// LWL shifts the input left so the addressed byte is highest
				preserved = (3 - (addr & 0x3)) * 8; // number of bits to
				// preserve
				mask = -1 << preserved; // preserved bits are 0 in mask
				dst = value << preserved; // shift input to correct place
				addr &= ~0x3;

				break;

			case Mips.LWR:
				value = memory.readMem(addr & ~0x3, 4);

				// LWR shifts the input right so the addressed byte is lowest
				preserved = (addr & 0x3) * 8; // number of bits to preserve
				mask = -1 >>> preserved; // preserved bits are 0 in mask
				dst = value >>> preserved; // shift input to correct place
				addr &= ~0x3;

				break;

				case Mips.STORE:
					memory.writeMem(addr, size, (int) src2);
					break;

				case Mips.SWL:
					value = memory.readMem(addr & ~0x3, 4);

					// SWL shifts highest order byte into the addressed position
					preserved = (3 - (addr & 0x3)) * 8;
					mask = -1 >>> preserved;
					dst = src2 >>> preserved;

					// merge values
					dst = (dst & mask) | (value & ~mask);

					memory.writeMem(addr & ~0x3, 4, (int) dst);
					break;

			case Mips.SWR:
				value = memory.readMem(addr & ~0x3, 4);

				// SWR shifts the lowest order byte into the addressed position
				preserved = (addr & 0x3) * 8;
				mask = -1 << preserved;
				dst = src2 << preserved;

				// merge values
				dst = (dst & mask) | (value & ~mask);

				memory.writeMem(addr & ~0x3, 4, (int) dst);
				break;

			case Mips.UNIMPL:
				System.err.println("Warning: encountered unimplemented inst");

			case Mips.INVALID:
				
				EmulatorHelpers.print(this);
				
				throw new MipsException(this, memory, MipsException.exceptionIllegalInstruction);

			default:
				Lib.assertNotReached();
		}
		

	}

	private void writeBack() throws MipsException {
		// if instruction is signed, but carry bit !+ sign bit, throw
		if (test(Mips.OVERFLOW) && Lib.test(dst, 31) != Lib.test(dst, 32))
			throw new MipsException(this, memory, MipsException.exceptionOverflow);

		if (test(Mips.DELAYEDLOAD))
			memory.delayedLoad(dstReg, (int) dst, mask);
		else
			memory.finishLoad();

		if (test(Mips.LINK))
			dst = nextPC;

		if (test(Mips.DST) && dstReg != 0)
			registers[dstReg] = (int) dst;

		if ((test(Mips.DST) || test(Mips.DELAYEDLOAD)) && dstReg != 0) {
			if (Lib.test(dbgFullDisassemble)) {
				System.out.print("#0x" + Lib.toHexString((int) dst));
				if (test(Mips.DELAYEDLOAD))
					System.out.print(" (delayed load)");
			}
		}

		if (test(Mips.BRANCH) && branch) {
			nextPC = jtarget;
		}

		advancePC(nextPC);
	}

	// state used to execute a single instruction
	int value, op, rs, rt, rd, sh, func, target, imm;
	int operation, format, flags;
	String name;

	int size;
	int addr, nextPC, jtarget, dstReg;
	long src1, src2, dst;
	int mask;
	boolean branch;

}
