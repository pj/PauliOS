package emulator;

import machine.Configuration;
import machine.Lib;
import machine.Page;

public class Memory {
	/** The registered target of the delayed load currently in progress. */
	private int loadTarget = 0;
	/** The bits to be modified by the delayed load currently in progress. */
	private int loadMask;
	/** The value to be loaded by the delayed load currently in progress. */
	private int loadValue;

	/**
	 * Either an associative or direct-mapped set of translation entries,
	 * depending on whether there is a TLB.
	 */
	public Page[] pages;

	/** Main memory for user programs. */
	public byte[] mainMemory;
	
	public Processor processor;
	
	/** Virtual memory disabled or enabled */
	public boolean vmEnabled = false;
	
	public Memory(int numPhysPages) {
		Configuration.numPhysPages = numPhysPages;

		mainMemory = new byte[Configuration.pageSize * numPhysPages];

	}

	/**
	 * Set the page table pointer. All further address translations will use the
	 * specified page table. The size of the current address space will be
	 * determined from the length of the page table array.
	 * 
	 * @param pageTable
	 *            the page table to use.
	 */
	public void setPageTable(Page[] pageTable) {
		this.pages = pageTable;
	}
	
	/**
	 * Return the number of pages of physical memory attached to this simulated
	 * processor.
	 * 
	 * @return the number of pages of physical memory.
	 */
	public int getNumPhysPages() {
		return Configuration.numPhysPages;
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
		return (page * Configuration.pageSize) | offset;
	}

	/**
	 * Extract the page number component from a 32-bit address.
	 * 
	 * @param address
	 *            the 32-bit address.
	 * @return the page number component of the address.
	 */
	public static int pageFromAddress(int address) {
		return (int) (((long) address & 0xFFFFFFFFL) / Configuration.pageSize);
	}

	/**
	 * Extract the offset component from an address.
	 * 
	 * @param address
	 *            the 32-bit address.
	 * @return the offset component of the address.
	 */
	public static int offsetFromAddress(int address) {
		return (int) (((long) address & 0xFFFFFFFFL) % Configuration.pageSize);
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
	private int newTranslate(int vaddr, int size, boolean writing) throws MipsException {		
		// check alignment
		if ((vaddr & (size - 1)) != 0) {
			throw new MipsException(processor, this, MipsException.exceptionAddressError, vaddr);
		}
		
		// is virtual memory enabled?
		if(vmEnabled){
			// calculate virtual page number and offset from the virtual address
			int vpn = pageFromAddress(vaddr);
			int offset = offsetFromAddress(vaddr);
			
			Page entry = pages[vpn];
			
			//System.out.println("vpn " + vpn);
			
			if(entry == null || !entry.present){
				throw new MipsException(processor, this, MipsException.exceptionPageFault, vaddr);
			}
	
			// check if trying to write a read-only page
			if (entry.readOnly && writing) {
				throw new MipsException(processor, this, MipsException.exceptionReadOnly, vaddr);
			}
	
			// check if physical page number is out of range
			int ppn = entry.ppn;
			if (ppn < 0 || ppn >= Configuration.numPhysPages) {
				throw new MipsException(processor, this, MipsException.exceptionBusError, vaddr);
			}
	
			// set used and dirty bits as appropriate
			entry.used = true;
			if (writing)
				entry.dirty = true;
	
			int paddr = (ppn * Configuration.pageSize) + offset;
	
			return paddr;
		}else{
			int ppn = pageFromAddress(vaddr);
			
			if (ppn < 0 || ppn >= Configuration.numPhysPages) {
				throw new MipsException(processor, this, MipsException.exceptionBusError, vaddr);
			}
			
			return vaddr;
		}
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
	public int readMem(int vaddr, int size) throws MipsException {
		int value = Lib.bytesToInt(mainMemory, newTranslate(vaddr, size, false), size);

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
	public void writeMem(int vaddr, int size, int value) throws MipsException {
		Lib.bytesFromInt(mainMemory, newTranslate(vaddr, size, true), size, value);
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
