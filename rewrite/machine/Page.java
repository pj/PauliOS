// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package machine;


/**
 * A single translation between a virtual page and a physical page.
 */
public final class Page {
	/**
	 * Allocate a new invalid translation entry.
	 */
	public Page() {
		present = false;
	}

	/**
	 * Allocate a new translation entry with the specified initial state.
	 * 
	 * @param vpn
	 *            the virtual page numben.
	 * @param ppn
	 *            the physical page number.
	 * @param valid
	 *            the valid bit.
	 * @param readOnly
	 *            the read-only bit.
	 * @param used
	 *            the used bit.
	 * @param dirty
	 *            the dirty bit.
	 */
	public Page(int vpn, int ppn, boolean present, boolean readOnly,
			boolean used, boolean dirty) {
		this.vpn = vpn;
		this.ppn = ppn;
		this.present = present;
		this.readOnly = readOnly;
		this.used = used;
		this.dirty = dirty;
	}

	/**
	 * Allocate a new translation entry, copying the contents of an existing
	 * one.
	 * 
	 * @param entry
	 *            the translation entry to copy.
	 */
	public Page(Page entry) {
		vpn = entry.vpn;
		ppn = entry.ppn;
		present = entry.present;
		readOnly = entry.readOnly;
		used = entry.used;
		dirty = entry.dirty;
	}

	/** The virtual page number. */
	public int vpn;

	/** The physical page number. */
	public int ppn;

	/**
	 * If this flag is <tt>false</tt>, this translation entry is ignored.
	 */
	public boolean present;

	/**
	 * If this flag is <tt>true</tt>, the user pprogram is not allowed to modify
	 * the contents of this virtual page.
	 */
	public boolean readOnly;

	/**
	 * This flag is set to <tt>true</tt> every time the page is read or written
	 * by a user program.
	 */
	public boolean used;

	/**
	 * This flag is set to <tt>true</tt> every time the page is written by a
	 * user program.
	 */
	public boolean dirty;
	
	/**
	 * The data the page contains
	 */
	public byte[] data = new byte[Configuration.pageSize];
	
	/**
	 * Process id that page belongs to
	 */
	public int pid;
}
