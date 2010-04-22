package filesystem;

import machine.Configuration;
import machine.Lib;

public class FileTableEntry {
	public FileTableEntry(String name2, int firstBlock, int length) {
		this.name = name2;
		this.firstBlock = firstBlock;
		this.length = length;
	}
	public String name; // file name limited to 16 chars
	public int firstBlock; // first block number
	
	public int openCount = 0; // number of processes that have this open
	
	public boolean deleting = false; // are we deleting this file?
	
	public int length;
	
	
	public FileTableEntry(byte[] entryBytes){
		firstBlock = Lib.bytesToInt(entryBytes, Configuration.fileFirstBlockOffset, Configuration.fileFirstBlockLength);
		length = Lib.bytesToInt(entryBytes, Configuration.fileLengthOffset, Configuration.fileLengthLength);
		
		name = Lib.bytesToString(entryBytes, Configuration.fileNameOffset, Configuration.fileNameLength);
	}
	
	public byte[] toBytes(){
		
		byte[] data = new byte[Configuration.fileEntrySize];
		
		int nameLength = Configuration.fileNameLength;
		
		if(nameLength > name.getBytes().length){
			nameLength = name.getBytes().length;
		}
		
		System.arraycopy(name.getBytes(), 0, data, Configuration.fileNameOffset, nameLength);
		
		System.arraycopy(Lib.bytesFromInt(length), 0, data, Configuration.fileLengthOffset, Configuration.fileLengthLength);
		
		System.arraycopy(Lib.bytesFromInt(firstBlock), 0, data, Configuration.fileFirstBlockOffset, Configuration.fileFirstBlockLength);
		
		return data;
	}
}
