package coff;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;


import emulator.Memory;
import machine.Lib;
import machine.Machine;
import machine.TranslationEntry;

public class CoffLoader {
	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @param pageTable 
	 * @return 
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	public CoffStartData load(InputStream fileInput, String[] args, TranslationEntry[] pageTable) throws CoffLoadException {
		int numPages;
		int stackPages = 8;
		
		CoffStartData initData = new CoffStartData();
		
		Coff coff = openCoffFile(fileInput);

		// make sure the sections are contiguous and start at page 0
		numPages = checkContiguous(coff);

		// make sure the argv array will fit in one page
		byte[][] argv_b = checkWillFit(args, coff);

		// program counter initially points at the program entry point
		initData.initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initData.initialSP = numPages * Memory.pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		loadSections(numPages, coff, pageTable);

		// store arguments in last page
		int entryOffset = (numPages - 1) * Memory.pageSize;
		int stringOffset = entryOffset + args.length * 4;

		initData.argc = args.length;
		initData.argv = entryOffset;

		for (int i = 0; i < argv_b.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			writeVirtualMemory(entryOffset, stringOffsetBytes);
			entryOffset += 4;
			writeVirtualMemory(stringOffset, argv_b[i]);
			stringOffset += argv_b[i].length;
			writeVirtualMemory(stringOffset, new byte[] { 0 });
			stringOffset += 1;
		}
		
		return initData;
	}

	private byte[][] checkWillFit(String[] args, Coff coff) throws CoffLoadException {
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > Memory.pageSize) {
			coff.close();
			throw new CoffLoadException("Process arguments too long");
		}
		return argv;
	}

	private int checkContiguous(Coff coff) throws CoffLoadException {
		int numPages;
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				throw new CoffLoadException("Fragmented Executable");
			}
			numPages += section.getLength();
		}
		return numPages;
	}

	private Coff openCoffFile(InputStream fileInput) throws CoffLoadException {
		Coff coff = null;
		
		//OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (fileInput == null) {
			throw new CoffLoadException("no input for coffFile");
		}

		// read input into array
		try {
			byte[] input = IOUtils.toByteArray(fileInput);
			
			return new Coff(input);
		} catch (IOException e) {
			throw new CoffLoadException("unable to read file");
		}
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * @param numPages 
	 * @param coff 
	 * @param pageTable 
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 * @throws CoffLoadException 
	 */
	protected void loadSections(int numPages, Coff coff, TranslationEntry[] pageTable) throws CoffLoadException {
		int numPhysPages = Machine.memory().getNumPhysPages();

		if (numPages > numPhysPages) {
			coff.close();
			throw new CoffLoadException("\tinsufficient physical memory");
		}

		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				loadPage(i, vpn, section, coff.file);
			}
		}
	}
	
	/**
	 * Load a page from this segment into physical memory.
	 * 
	 * @param spn
	 *            the page number within this segment.
	 * @param ppn
	 *            the physical page to load into.
	 * @return <tt>true</tt> if successful.
	 */
	public void loadPage(int spn, int ppn, CoffSection section, byte[] file) {
		int pageSize = Memory.pageSize;
		byte[] memory = Machine.memory().mainMemory;
		int paddr = ppn * pageSize;
		int faddr = section.contentOffset + spn * pageSize;
		int initlen;

		if (!section.isInitialzed())
			initlen = 0;
		else if (spn == section.numPages - 1)
			initlen = section.size % pageSize;
		else
			initlen = pageSize;

		System.arraycopy(file, faddr, memory, paddr, initlen);
		
		//Lib.strictReadFile(file, faddr, memory, paddr, initlen);

		Arrays.fill(memory, paddr + initlen, paddr + pageSize, (byte) 0);
	}
	
	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		byte[] memory = Machine.memory().mainMemory;

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		byte[] memory = Machine.memory().mainMemory;

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
	}
}
