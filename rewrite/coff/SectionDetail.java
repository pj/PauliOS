package coff;

public class SectionDetail {
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
