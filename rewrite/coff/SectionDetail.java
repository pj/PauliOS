package coff;

/**
 * Holds details about a coff section for loading into memory.
 * 
 * @author pauljohnson
 *
 */
public class SectionDetail {
	// The name of the section such as ".text" or ".data"
	public String name;
	public int vaddr;

	public int size;

	public int contentOffset;

	public int flags;
	public int numRelocations;

	public boolean executable;
	public boolean readOnly;

	public boolean initialized;
	public int numPages;

	public int firstVPN;
}
