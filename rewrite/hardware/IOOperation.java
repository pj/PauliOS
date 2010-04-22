package hardware;

import java.io.File;

import kernel.PCB;

public class IOOperation {
	public int action;
	
	public int pointer;
	
	public int length;
	
	public int rval;
	
	// data to read/write
	public byte[] rdata;
	
	
	// file system stuff
	public int position;
}
