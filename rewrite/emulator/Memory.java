package emulator;

import machine.Config;
import machine.Lib;
import machine.TranslationEntry;

public class Memory {
	/** The registered target of the delayed load currently in progress. */
	private int loadTarget = 0;
	/** The bits to be modified by the delayed load currently in progress. */
	private int loadMask;
	/** The value to be loaded by the delayed load currently in progress. */
	private int loadValue;

	/** <tt>true</tt> if using a software-managed TLB. */
	private boolean usingTLB;
	/** Number of TLB entries. */
	private int tlbSize = 4;
	/**
	 * Either an associative or direct-mapped set of translation entries,
	 * depending on whether there is a TLB.
	 */
	private TranslationEntry[] translations;

	/** Size of a page, in bytes. */
	public static final int pageSize = 0x400;
	/** Number of pages in a 32-bit address space. */
	private static final int maxPages = (int) (0x100000000L / pageSize);
	/** Number of physical pages in memory. */
	private int numPhysPages;
	/** Main memory for user programs. */
	public byte[] mainMemory;
	
	private Processor processor;
	
	public Memory(Processor processor, int numPhysPages) {
		this.processor = processor;
		
		usingTLB = Config.getBoolean("Processor.usingTLB");

		this.numPhysPages = numPhysPages;

		mainMemory = new byte[pageSize * numPhysPages];

		if (usingTLB) {
			translations = new TranslationEntry[tlbSize];
			for (int i = 0; i < tlbSize; i++)
				translations[i] = new TranslationEntry();
		} else {
			translations = null;
		}
	}
	
	
	/**
	 * Test whether this processor uses a software-managed TLB, or single-level
	 * paging.
	 * 
	 * <p>
	 * If <tt>false</tt>, this processor directly supports single-level paging;
	 * use <tt>setPageTable()</tt>.
	 * 
	 * <p>
	 * If <tt>true</tt>, this processor has a software-managed TLB; use
	 * <tt>getTLBSize()</tt>, <tt>readTLBEntry()</tt>, and
	 * <tt>writeTLBEntry()</tt>.
	 * 
	 * <p>
	 * Using a method associated with the wrong address translation mechanism
	 * will result in an assertion failure.
	 * 
	 * @return <tt>true</tt> if this processor has a software-managed TLB.
	 */
	public boolean hasTLB() {
		return usingTLB;
	}

	/**
	 * Set the page table pointer. All further address translations will use the
	 * specified page table. The size of the current address space will be
	 * determined from the length of the page table array.
	 * 
	 * @param pageTable
	 *            the page table to use.
	 */
	public void setPageTable(TranslationEntry[] pageTable) {
		this.translations = pageTable;
	}

	/**
	 * Return the number of entries in this processor's TLB.
	 * 
	 * @return the number of entries in this processor's TLB.
	 */
	public int getTLBSize() {
		return tlbSize;
	}

	/**
	 * Returns the specified TLB entry.
	 * 
	 * @param number
	 *            the index into the TLB.
	 * @return the contents of the specified TLB entry.
	 */
	public TranslationEntry readTLBEntry(int number) {
		return new TranslationEntry(translations[number]);
	}

	/**
	 * Fill the specified TLB entry.
	 * 
	 * <p>
	 * The TLB is fully associative, so the location of an entry within the TLB
	 * does not affect anything.
	 * 
	 * @param number
	 *            the index into the TLB.
	 * @param entry
	 *            the new contents of the TLB entry.
	 */
	public void writeTLBEntry(int number, TranslationEntry entry) {
		translations[number] = new TranslationEntry(entry);
	}

	/**
	 * Return the number of pages of physical memory attached to this simulated
	 * processor.
	 * 
	 * @return the number of pages of physical memory.
	 */
	public int getNumPhysPages() {
		return numPhysPages;
	}

	/**
	 * Return a reference to the physical memory array. The size of this array
	 * is <tt>pageSize * getNumPhysPages()</tt>.
	 * 
	 * @return the main memory array.
	 */
	public byte[] getMemory() {
		return mainMemory;
	}

	/**
	 * Concatenate a page number and an offset into an address.
	 * 
	 * @param page
	 *            the page number. Must be between <tt>0</tt> and
	 *            <tt>(2<sup>32</sup> / pageSize) - 1</tt>.
	 * @param offset
	 *            the offset within the page. Must be between <tt>0</tt> and
	 *            <tt>pageSize - 1</tt>.
	 * @return a 32-bit address consisting of the specified page and offset.
	 */
	public static int makeAddress(int page, int offset) {
		return (page * pageSize) | offset;
	}

	/**
	 * Extract the page number component from a 32-bit address.
	 * 
	 * @param address
	 *            the 32-bit address.
	 * @return the page number component of the address.
	 */
	public static int pageFromAddress(int address) {
		return (int) (((long) address & 0xFFFFFFFFL) / pageSize);
	}

	/**
	 * Extract the offset component from an address.
	 * 
	 * @param address
	 *            the 32-bit address.
	 * @return the offset component of the address.
	 */
	public static int offsetFromAddress(int address) {
		return (int) (((long) address & 0xFFFFFFFFL) % pageSize);
	}

	void finishLoad() {
		delayedLoad(0, 0, 0);
	}

	/**
	 * Translate a virtual address into a physical address, using either a page
	 * table or a TLB. Check for alignment, make sure the virtual page is valid,
	 * make sure a read-only page is not being written, make sure the resulting
	 * physical page is valid, and then return the resulting physical address.
	 * 
	 * @param vaddr
	 *            the virtual address to translate.
	 * @param size
	 *            the size of the memory reference (must be 1, 2, or 4).
	 * @param writing
	 *            <tt>true</tt> if the memory reference is a write.
	 * @return the physical address.
	 * @exception MipsException
	 *                if a translation error occurred.
	 */
	private int translate(int vaddr, int size, boolean writing)
			throws MipsException {
		// check alignment
		if ((vaddr & (size - 1)) != 0) {
			throw new MipsException(processor, this, MipsException.exceptionAddressError, vaddr);
		}

		// calculate virtual page number and offset from the virtual address
		int vpn = pageFromAddress(vaddr);
		int offset = offsetFromAddress(vaddr);

		TranslationEntry entry = null;

		// if not using a TLB, then the vpn is an index into the table
		if (!usingTLB) {
			if (vpn >= translations.length || translations[vpn] == null
					|| !translations[vpn].valid) {
				throw new MipsException(processor, this, MipsException.exceptionPageFault, vaddr);
			}

			entry = translations[vpn];
		}
		// else, look through all TLB entries for matching vpn
		else {
			for (int i = 0; i < tlbSize; i++) {
				if (translations[i].valid && translations[i].vpn == vpn) {
					entry = translations[i];
					break;
				}
			}
			if (entry == null) {
				throw new MipsException(processor, this, MipsException.exceptionTLBMiss, vaddr);
			}
		}

		// check if trying to write a read-only page
		if (entry.readOnly && writing) {
			throw new MipsException(processor, this, MipsException.exceptionReadOnly, vaddr);
		}

		// check if physical page number is out of range
		int ppn = entry.ppn;
		if (ppn < 0 || ppn >= numPhysPages) {
			throw new MipsException(processor, this, MipsException.exceptionBusError, vaddr);
		}

		// set used and dirty bits as appropriate
		entry.used = true;
		if (writing)
			entry.dirty = true;

		int paddr = (ppn * pageSize) + offset;

		return paddr;
	}

	/**
	 * Read </i>size</i> (1, 2, or 4) bytes of virtual memory at <i>vaddr</i>,
	 * and return the result.
	 * 
	 * @param vaddr
	 *            the virtual address to read from.
	 * @param size
	 *            the number of bytes to read (1, 2, or 4).
	 * @return the value read.
	 * @exception MipsException
	 *                if a translation error occurred.
	 */
	int readMem(int vaddr, int size) throws MipsException {
		int value = Lib.bytesToInt(mainMemory, translate(vaddr, size, false), size);

		return value;
	}

	/**
	 * Write <i>value</i> to </i>size</i> (1, 2, or 4) bytes of virtual memory
	 * starting at <i>vaddr</i>.
	 * 
	 * @param vaddr
	 *            the virtual address to write to.
	 * @param size
	 *            the number of bytes to write (1, 2, or 4).
	 * @param value
	 *            the value to store.
	 * @exception MipsException
	 *                if a translation error occurred.
	 */
	void writeMem(int vaddr, int size, int value) throws MipsException {
		Lib.bytesFromInt(mainMemory, translate(vaddr, size, true), size, value);
	}

	/**
	 * Complete the in progress delayed load and scheduled a new one.
	 * 
	 * @param nextLoadTarget
	 *            the target register of the new load.
	 * @param nextLoadValue
	 *            the value to be loaded into the new target.
	 * @param nextLoadMask
	 *            the mask specifying which bits in the new target are to be
	 *            overwritten. If a bit in <tt>nextLoadMask</tt> is 0, then the
	 *            corresponding bit of register <tt>nextLoadTarget</tt> will not
	 *            be written.
	 */
	void delayedLoad(int nextLoadTarget, int nextLoadValue,
			int nextLoadMask) {
		// complete previous delayed load, if not modifying r0
		if (loadTarget != 0) {
			int savedBits = processor.registers[loadTarget] & ~loadMask;
			int newBits = loadValue & loadMask;
			processor.registers[loadTarget] = savedBits | newBits;
		}

		// schedule next load
		loadTarget = nextLoadTarget;
		loadValue = nextLoadValue;
		loadMask = nextLoadMask;
	}
}
