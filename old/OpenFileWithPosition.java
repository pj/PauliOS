// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package filesystem;

/**
 * An <tt>OpenFile</tt> that maintains a current file position.
 */
public abstract class OpenFileWithPosition extends OpenFile {
	/**
	 * Allocate a new <tt>OpenFileWithPosition</tt>.
	 */
	public OpenFileWithPosition() {
	}

	public void seek(int position) {
		this.position = position;
	}

	public int tell() {
		return position;
	}

	public int read(byte[] buf, int offset, int length) {
		int amount = read(position, buf, offset, length);
		if (amount == -1)
			return -1;

		position += amount;
		return amount;
	}

	public int write(byte[] buf, int offset, int length) {
		int amount = write(position, buf, offset, length);
		if (amount == -1)
			return -1;

		position += amount;
		return amount;
	}

	/**
	 * The current value of the file pointer.
	 */
	protected int position = 0;
}
