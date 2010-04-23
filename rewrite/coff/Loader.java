package coff;

import machine.Configuration;
import machine.Lib;
import machine.Page;
import emulator.Memory;
import emulator.MipsException;
import filesystem.FileSystem;
import kernel.Kernel;
import kernel.PCB;

/**
 * Loads a c program file from disk into a processes memory
 * 
 * @author pauljohnson
 *
 */
public class Loader {

	// to start the program
	public int stackPointer;
	public int programCounter;
	public int argc;
	public int argv;
	
	// shared 
	public PCB process;
	public FileSystem fs;
	Kernel kernel;
	Memory memory;
	int fid;

	
	public void load(int fid, PCB process, FileSystem fs, Kernel kernel, Memory memory, String[] args) throws MipsException, CoffLoadException{	
		this.process = process;
		this.fs = fs;
		this.kernel = kernel;
		this.memory = memory;
		this.fid = fid;
		
		// read headers from file to memory
		int headersRead = fs.read(fid, Configuration.totalHeaderLength, 0, kernel, process);
		
		checkHeaderLength(headersRead);
		
		// read header values
		int magic = memory.readMem(0, 2);//Lib.bytesToUnsignedShort(headers, 0);
		int numSections = memory.readMem(2, 2);//Lib.bytesToUnsignedShort(headers, 2);
		int optionalHeaderLength = memory.readMem(16, 2);//Lib.bytesToUnsignedShort(headers, 16);
		int flags = memory.readMem(18, 2);//Lib.bytesToUnsignedShort(headers, 18);
		programCounter = memory.readMem(Configuration.headerLength+16, 4);//Lib.bytesToInt(headers, headerLength + 16);
		
		checkHeaderDetails(magic, numSections, flags);
		
		byte[][] argv_b = checkWillFit(args);
		
		// load sections
		int numPages = loadSections(numSections, optionalHeaderLength);
		
		// next comes the stack; stack pointer initially points to top of it
		numPages += Configuration.stackPages;
		stackPointer = numPages * Configuration.pageSize;

		// create stack pages if necessary
		for(int i = numPages - Configuration.stackPages; i <= numPages; i++){
			if(process.pageTable[i] == null){
				process.pageTable[i] = new Page(i, -1, false, false, false, false);
			}
		}
		
		// and finally reserve 1 page for arguments
		numPages++;
		
		if(process.pageTable[numPages] == null){
			process.pageTable[numPages] = new Page(numPages, -1, false, false, false, false);
		}
		
		//System.out.println(numPages);
		
		// write arguments to memory
		
		// store arguments in last page
				
		int argvPointer = Memory.makeAddress(numPages, 0);
		int stringsStartPointer = argvPointer + args.length * 4;

		//System.out.println("argvPointer: " + Integer.toHexString(argvPointer));
		
		kernel.checkInMemory(argvPointer);
		
		argc = args.length;
		argv = argvPointer;

		for (int i = 0; i < argv_b.length; i++) {
			// write string pointer
			memory.writeMem(argvPointer, 4, stringsStartPointer);
			argvPointer += 4;
			
			// write string
			for(int j = 0; j < argv_b[i].length; j++){
				memory.writeMem(stringsStartPointer, 1, argv_b[i][j]);
				stringsStartPointer++;
			}
			
			
			// write 0 at end of string
			memory.writeMem(stringsStartPointer, 1, 0);
			stringsStartPointer++;
		}
	}
	
	private int loadSections(int numSections, int optionalHeaderLength) throws CoffLoadException, MipsException {
		int numPages = 0;
		
		int sectionTableOffset = Configuration.headerLength + optionalHeaderLength;
		
		//System.out.println("Section table offset: " + sectionTableOffset);
		
		// set file positon
		fs.seek(fid, sectionTableOffset, process);
		
		// read section headers to memory
		int sectionHeaderRead = fs.read(fid, Configuration.coffSectionHeaderLength*numSections, 0, kernel, process);
		
		// check that section headers size is correct
		if (Configuration.coffSectionHeaderLength*numSections != sectionHeaderRead) {
			throw new CoffLoadException("section header truncated");
		}
		
		SectionDetail[] sections = new SectionDetail[numSections];
		
		for (int s = 0; s < numSections; s++) {
			SectionDetail section = new SectionDetail();
			sections[s] = section;
			
			// this is the offset into memory where the sectionheader for section s is
			int sectionEntryOffset = s * Configuration.coffSectionHeaderLength;
			
			byte[] namebuf = new byte[8];
			
			for(int i = 0; i < namebuf.length; i++){
				namebuf[i] = (byte) memory.readMem(sectionEntryOffset+i, 1);
			}
			
			section.name = Lib.bytesToString(namebuf, 0, 8);
			
			//System.out.println("Section Name: " + section.name);
			
			section.vaddr = memory.readMem(sectionEntryOffset+12, 4);//Lib.bytesToInt(buf, 12);
			section.size = memory.readMem(sectionEntryOffset+16, 4);//Lib.bytesToInt(buf, 16);
			section.contentOffset = memory.readMem(sectionEntryOffset+20, 4);//Lib.bytesToInt(buf, 20);
			
			//System.out.println("Section offset: " + section.contentOffset);
			
			section.numRelocations = memory.readMem(sectionEntryOffset+32, 2);//Lib.bytesToUnsignedShort(buf, 32);
			section.flags = memory.readMem(36, 4);//Lib.bytesToInt(buf, 36);

			checkRelocations(section.numRelocations);
			checkSectionAddresses(section.vaddr, section.contentOffset, section.size);
			
			boolean[] bflags = loadFlags(section.flags);
			
			section.executable = bflags[0];
			section.readOnly = bflags[1];
			section.initialized = bflags[2];
			
			section.numPages = Lib.divRoundUp(section.size, Configuration.pageSize);
			section.firstVPN = section.vaddr / Configuration.pageSize;
			
			checkFragmentedExecutable(section.firstVPN, numPages);
			

			numPages += section.numPages;
		}
		
		// now load the pages from the file
		
		for(int i = 0; i < sections.length; i++){
			SectionDetail section = sections[i];
			
			
			loadSection(section);
		}
		
		return numPages;
	}
	
	protected void loadSection(SectionDetail section) throws CoffLoadException, MipsException {
		
		int fileStart = section.contentOffset;
		
		for (int i = 0; i < section.numPages; i++) {
			int vpn = section.firstVPN + i;
			
			//System.out.println("Loading sections: " + vpn);
			
			// create page
			if(process.pageTable[vpn] == null){
				process.pageTable[vpn] = new Page(vpn, -1, false, false, false, false);
			}

			
			//System.out.println("Section file start: " + fileStart);
			
			int memoryPointer = Memory.makeAddress(vpn, 0);
			
			kernel.checkInMemory(memoryPointer);
			
			//System.out.println("Physical page number: " + process.pageTable[vpn].ppn);
			
			fs.seek(fid, fileStart, process);

			fs.read(fid, Configuration.pageSize, memoryPointer, kernel, process);
			
			//System.out.println("First Instruction: " + Integer.toHexString(memory.readMem(memoryPointer, 4)));
			
			// initialize data
			
			int initlen;
			
			if (!section.initialized){
				initlen = 0;
			}else if (i == section.numPages - 1){
				initlen = section.size % Configuration.pageSize;
			}else{
				initlen = Configuration.pageSize;
			}
			
			for(int j = initlen; j < Configuration.pageSize; j++){
				memory.writeMem(memoryPointer+j, 1, 0);
			}
			
			 fileStart += Configuration.pageSize;
		}

	}
	
	private boolean[] loadFlags(int flags) throws CoffLoadException {
		boolean executable;
		boolean readOnly;
		boolean initialized;
		switch (flags & 0x0FFF) {
		case 0x0020:
			executable = true;
			readOnly = true;
			initialized = true;
			break;
		case 0x0040:
			executable = false;
			readOnly = false;
			initialized = true;
			break;
		case 0x0080:
			executable = false;
			readOnly = false;
			initialized = false;
			break;
		case 0x0100:
			executable = false;
			readOnly = true;
			initialized = true;
			break;
		default:
			throw new CoffLoadException("invalid section flags: " + flags);
		}
		
		return new boolean[]{executable, readOnly, initialized};
	}

	private void checkSectionAddresses(int vaddr, int contentOffset, int size) throws CoffLoadException {
		if (vaddr % Configuration.pageSize != 0 || size < 0 || contentOffset < 0
				|| contentOffset + size > process.files[fid].entry.length) {
			throw new CoffLoadException("invalid section addresses: "
					+ "vaddr=" + vaddr + " size=" + size + " contentOffset="
					+ contentOffset);
		}
	}

	private void checkRelocations(int numRelocations) throws CoffLoadException {
		if (numRelocations != 0) {
			throw new CoffLoadException("section needs relocation");
		}
	}

	private void checkFragmentedExecutable(int firstVPN, int numPages) throws CoffLoadException {
		if (firstVPN != numPages) {
			throw new CoffLoadException("Fragmented Executable");
		}
	}

	public void checkHeaderLength(int headersRead) throws CoffLoadException{
		if (headersRead < Configuration.totalHeaderLength) {
			throw new CoffLoadException("file is not executable");
		}
	}
	
	public void checkHeaderDetails(int magic, int numSections, int flags) throws CoffLoadException{
		if (magic != 0x0162) {
			throw new CoffLoadException("incorrect magic number");
		}
		if (numSections < 2 || numSections > 10) {
			throw new CoffLoadException("bad section count");
		}
		if ((flags & 0x0003) != 0x0003) {
			throw new CoffLoadException("bad header flags");
		}
	}
	
	private byte[][] checkWillFit(String[] args) throws CoffLoadException {
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > Configuration.pageSize) {
			throw new CoffLoadException("Process arguments too long");
		}
		return argv;
	}
}
