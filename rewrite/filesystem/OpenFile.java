package filesystem;

public class OpenFile {
	public OpenFile(String name2, FileTableEntry entry) {
		this.name = name2;
		this.entry = entry;
	}
	public String name; // file name limited to 16 chars
	public int position = 0; // current file position
	
	public FileTableEntry entry;
}
