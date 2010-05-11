package filesystem;

/**
 * This represents a file that is open by a process
 * 
 * @author pauljohnson
 *
 */
public class OpenFile {
	public OpenFile(String name2, FileTableEntry entry) {
		this.name = name2;
		this.entry = entry;
	}
	public String name; // file name limited to 16 chars
	public int position = 0; // current read/write file position
	
	/**
	 * The disk entry this OpenFile is associated with - 
	 * note this entry could be open by several processes
	 */
	public FileTableEntry entry;
}
