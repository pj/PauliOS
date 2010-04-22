package filesystem;

import machine.Machine;
import kernel.Kernel;
import kernel.PCB;

public interface FileSystem {
	
	public int open(String name, PCB pcb);

	public int close(int fid, PCB process);
	
	public int create(String name, PCB process);
	
	public int read(int fid, int length, int bufferPointer, Kernel kernel, PCB process);
	
	public int write(int fid, int length, int startPointer, Kernel kernel, PCB process);
	
	public int unlink(String name);
	
	public int seek(int fid, int position, PCB process);
	
	public void initialize(Machine machine);
	
	public FileTableEntry[] getFiles();
}
