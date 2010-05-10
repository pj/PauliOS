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
	
	public void seek(int fid, int position, PCB process);
	
	public void initialize(Machine machine);
	
	public int chdir(String path, PCB process);
	
	public int mkdir(String path, PCB process);
	
	public int rmdir(String path, PCB process);
	
	public FileTableEntry[] getFiles();
}
