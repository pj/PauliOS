// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package coff;

import java.io.EOFException;
import java.io.InputStream;
import java.util.Arrays;

import emulator.Memory;

import machine.Machine;

import machine.Lib;


/**
 * A <tt>CoffSection</tt> manages a single section within a COFF executable.
 */
public class CoffSection {
	/**
	 * Load a COFF section from an executable.
	 * 
	 * @param file
	 *            the file containing the executable.
	 * @param headerOffset
	 *            the offset of the section header in the executable.
	 * 
	 * @exception EOFException
	 *                if an error occurs.
	 */
	public CoffSection(byte[] file, int headerOffset) throws CoffLoadException {
		this.file = file;

		if (headerOffset + headerLength > file.length) {
			throw new CoffLoadException("section header truncated");
		}

		byte[] buf = new byte[headerLength];
		
		System.arraycopy(file, headerOffset, buf, 0, headerLength);
		
		//Lib.strictReadFile(file, headerOffset, buf, 0, headerLength);

		name = Lib.bytesToString(buf, 0, 8);
		vaddr = Lib.bytesToInt(buf, 12);
		size = Lib.bytesToInt(buf, 16);
		contentOffset = Lib.bytesToInt(buf, 20);
		numRelocations = Lib.bytesToUnsignedShort(buf, 32);
		flags = Lib.bytesToInt(buf, 36);

		if (vaddr % Memory.pageSize != 0 || size < 0 || contentOffset < 0
				|| contentOffset + size > file.length) {
			throw new CoffLoadException("invalid section addresses: "
					+ "vaddr=" + vaddr + " size=" + size + " contentOffset="
					+ contentOffset);
		}

		if (numRelocations != 0) {
			throw new CoffLoadException("section needs relocation");
		}

		switch (flags & 0x0FFF) {
		case 0x0020:
			executable = true;
			readOnly = true;
			initialized = true;
			break;
		case 0x0040:
			executable = false;
			readOnly = false;
			initialized = true;
			break;
		case 0x0080:
			executable = false;
			readOnly = false;
			initialized = false;
			break;
		case 0x0100:
			executable = false;
			readOnly = true;
			initialized = true;
			break;
		default:
			throw new CoffLoadException("invalid section flags: " + flags);
		}

		numPages = Lib.divRoundUp(size, Memory.pageSize);
		firstVPN = vaddr / Memory.pageSize;
	}

	/**
	 * Return the name of this section.
	 * 
	 * @return the name of this section.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Test whether this section is read-only.
	 * 
	 * @return <tt>true</tt> if this section should never be written.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * Test whether this section is initialized. Loading a page from an
	 * initialized section requires a disk access, while loading a page from an
	 * uninitialized section requires only zero-filling the page.
	 * 
	 * @return <tt>true</tt> if this section contains initialized data in the
	 *         executable.
	 */
	public boolean isInitialzed() {
		return initialized;
	}

	/**
	 * Return the length of this section in pages.
	 * 
	 * @return the number of pages in this section.
	 */
	public int getLength() {
		return numPages;
	}

	/**
	 * Return the first virtual page number used by this section.
	 * 
	 * @return the first virtual page number used by this section.
	 */
	public int getFirstVPN() {
		return firstVPN;
	}

	private byte[] file;

	private String name;
	private int vaddr;

	int size;

	int contentOffset;

	private int flags;
	private int numRelocations;

	private boolean executable, readOnly;

	boolean initialized;
	int numPages;

	private int firstVPN;

	/** The length of a COFF section header. */
	public static final int headerLength = 40;

	private static final char dbgCoffSection = 'c';
}
