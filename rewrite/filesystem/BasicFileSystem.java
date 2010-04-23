package filesystem;

import hardware.HardDrive;
import hardware.IOOperation;
import hardware.Interrupt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import emulator.MipsException;

import machine.Configuration;
import machine.Lib;
import machine.Machine;
import kernel.Kernel;
import kernel.KernelFault;
import kernel.PCB;

public class BasicFileSystem implements FileSystem{
	/**
	 * Constants for error numbers
	 */
	public static final int non_existant = -1;
	
	/**
	 * File allocation table notes where the next block of the file is
	 */
	public int[] fat;
	
	public FileTableEntry[] files;
	
	public boolean[] availableBlocks;
	

	private Machine machine;

	/**
	 * Loads the file system from the hard drive and creates the necessary data structures to support file system operation.
	 */
	public void initialize(Machine machine){
		this.machine = machine;
		
		files = new FileTableEntry[Configuration.maxFiles];
		fat = new int[Configuration.numberOfBlocks];
		availableBlocks = new boolean[Configuration.numberOfBlocks];
		
		Arrays.fill(availableBlocks, false);
		
		// load filesystem from hard drive
		
		// load fat table
		byte[] fatTable = new byte[Configuration.fatLength];
		
		readDrive(Configuration.bootBlockLength, fatTable);		
		
		// process fat table
		for(int i = 0; i < Configuration.numberOfBlocks; i++){
			fat[i] = Lib.bytesToInt(fatTable, i*Configuration.fatEntrySize, Configuration.fatEntrySize);
		}
		
		// get number of file system entries from superblock
		byte[] b_entries = new byte[4];
		
		readDrive(Configuration.bootBlockLength + Configuration.fatLength, b_entries);
		int entryCount = Lib.bytesToInt(b_entries,0);
		
		// load entries from disk
		byte[] fileEntries = new byte[entryCount * Configuration.fileEntrySize];
		
		readDrive(Configuration.bootBlockLength + Configuration.fatLength + Configuration.superBlockSize, fileEntries);
		
		byte[] entryData = new byte[32];
		
		for(int i = 0; i < entryCount; i++){
			System.arraycopy(fileEntries, i*Configuration.fileEntrySize, entryData, 0, 32);
			files[i] = new FileTableEntry(entryData);
		}
	}
	
	/**
	 * Checks whether the file named name exists on the file system.
	 * 
	 * @param name
	 * @return
	 */
	FileTableEntry exists(String name){
		for(int i = 0; i < Configuration.maxFiles; i++){
			FileTableEntry entry = files[i];
			
//			if(entry != null)
//				System.out.println(entry.name);
			
			if(entry != null && entry.name.equals(name)){
				return entry;
			}
		}
		
		return null;
	}
	
	/**
	 * Gets the first available slot in a processes files.
	 * 
	 * @param files2
	 * @return
	 */
	public int getFid(OpenFile[] files2){
		for(int i = 2; i < Configuration.maxFiles; i++){
			if(files2[i] == null){
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Open a file into the process 
	 */
	public int open(String name, PCB process){
		FileTableEntry entry = exists(name);
		
		if(entry == null || entry.deleting){
			return -1;
		}
		
		int fid = getFid(process.files);
		
		process.files[fid] = new OpenFile(name, entry);
		
		entry.openCount++;
		
		return fid;
	}
	

	
	public int close(int fid, PCB process){	
		OpenFile entry = process.files[fid];

		if(entry == null){
			return -1;
		}
		
		entry.entry.openCount--;
		
		process.files[fid] = null;		
		
		return 0;
	}
	
	public int create(String name, PCB process){
		FileTableEntry entry = exists(name);
		
		if(entry == null){
			entry = new FileTableEntry(name, -1, 0);
			
			addFile(entry);
			
		}else if(entry.deleting){
			return -1;
		}
		
		int fid = getFid(process.files);
		
		// find free block to start
		int blockNumber = findFreeBlock();
		
		entry.firstBlock = blockNumber;
		entry.openCount++;
		
		fat[blockNumber] = -1;
		
		// create process openfile
		process.files[fid] = new OpenFile(entry.name, entry);
		
		// write entry to disk	
		writeDrive(Configuration.bootBlockLength + Configuration.fatLength + Configuration.superBlockSize + (fid * Configuration.fileEntrySize), entry.toBytes());
		
		// write fat table to disk
		writeDrive(Configuration.bootBlockLength  + (blockNumber * Configuration.fatEntrySize), Lib.bytesFromInt(-1));
		
		return fid;
	}



	public int read(int fid, int length, int bufferPointer, Kernel kernel, PCB process) {
		if(fid > Configuration.maxFiles || fid < 0){
			return -1;
		}
		
		OpenFile file = process.files[fid];
		
		if(file == null){
			return -1;
		}
		
		int startPosition = file.position;
		int endPosition = file.position + length;
		
		if(endPosition > file.entry.length){
			endPosition = file.entry.length;
		}
		
		// find first block to read
		int block = getFirstBlock(file);
		
		if(block == -1){
			return 0;
		}
		
		// calculate first blockOffset
		
		// blockOffset is the offset from the start of the current block usually 0
		int blockOffset = startPosition % Configuration.blockSize;
		
		// the amount of data to read from the block we are reading from
		int blockLength;
			
		if(length > Configuration.blockSize){
			blockLength = Configuration.blockSize - blockOffset;
		}else{
			blockLength = length;
		}
		
		// number of bytes read
		int read = 0;
		
		// physical position on disk - calculated from block number and block offset
		int diskPosition;
		
		while(file.position < endPosition){
			// read from disk into data array
			byte[] data = new byte[blockLength];
			
			diskPosition = Configuration.fileOffset + (block * Configuration.blockSize) + blockOffset;
			
			int rval = readDrive(diskPosition, data);
			
			// check if disk returned an error
			if(rval < 0){
				return rval;
			}
			
			// check bufferPointer page is in memory
			kernel.checkInMemory(bufferPointer);
			
			// copy read data into memory
			for(int i = 0; i < data.length; i++){
				try {
					machine.memory().writeMem(bufferPointer++, 1, data[i]);
				// bad address - should not be page fault here
				} catch (MipsException e) {
					return -1;
				}
			}
			
			// block offset here will always be 0 since the first block is the only one that can have something different
			blockOffset = 0;
			
			// set new position, read and block length
			file.position += blockLength;
			read += blockLength;
					
			if(endPosition - file.position > 1024){
				blockLength = 1024;
			}else{
				blockLength = endPosition - file.position;
			}
			
			// get next block
			block = fat[block];			
			
			// file system inconsistent
			if(block == -2){
				return -1;
			}
			
			// end of file reached
			if(block == -1){
				return read;
			}
		}
		
		return read;
	}

	private int getFirstBlock(OpenFile file) {
		int firstBlock = file.entry.firstBlock;
		
		int numBlocks = file.position / 1024;
		
		for(int i = 0; i < numBlocks; i++){
			firstBlock = fat[firstBlock];
		}
		
		return firstBlock;
	}
	
	/**
	 * Write from a buffer in memory to disk.
	 * 
	 */
	public int write(int fid, int length, int startPointer, Kernel kernel, PCB process) {
	
		if(fid > Configuration.maxFiles || fid < 0){
			return -1;
		}
		
		OpenFile file = process.files[fid];
		
		if(file == null){
			return -1;
		}
		
		int startPosition = file.position;
		int endPosition = file.position + length;
		
		byte[] data;
		
		int diskPosition;
		
		int block = getFirstBlock(file);
		
		// blockOffset is the offset from the start of the current block usually 0
		int blockOffset = startPosition % Configuration.blockSize;
		
		// the amount of data to read from the block we are reading from
		int blockLength;
			
		if(length > Configuration.blockSize){
			blockLength = Configuration.blockSize - blockOffset;
		}else{
			blockLength = length;
		}
		
		int prevBlock = -1;
		
		int read = 0;
		
		while(file.position < endPosition){
			// end of file reached -  need new block!
			// allocate one and update fat
			if(block == -1){
				// get new block
				int newBlock = findFreeBlock();
				
				fat[prevBlock] = newBlock;
				fat[newBlock] = -1;
				
				// write changes in fat to disk
				writeDrive(Configuration.bootBlockLength + (prevBlock * 4), Lib.bytesFromInt(newBlock));
				writeDrive(Configuration.bootBlockLength + (newBlock * 4), Lib.bytesFromInt(-1));
				
				block = newBlock;
			}
			
			// read from memory to data
			data = new byte[blockLength];
			
			kernel.checkInMemory(startPointer);
			
			for(int i = 0; i < blockLength; i++){
				try {
					data[i] = (byte)machine.memory().readMem(startPointer++, 1);
				} catch (MipsException e) {
					return -1;
				}
			}
			
			// write to disk
			diskPosition = Configuration.fileOffset + (block * Configuration.blockSize) + blockOffset;
				
			int rval = writeDrive(diskPosition, data);
			
			if(rval < 0){
				return -1;
			}
			
			// block offset here will always be 0 since the first block is the only one that can have something different
			blockOffset = 0;
			
			// set new position, read and block length
			file.position += blockLength;
			read += blockLength;
					
			if(endPosition - file.position > Configuration.blockSize){
				blockLength = Configuration.blockSize;
			}else{
				blockLength = endPosition - file.position;
			}
			
			// get next block
			prevBlock = block;
			block = fat[block];

			// file system inconsistent
			if(block == -2){
				return -1;
			}
		}
		
		if(file.position > file.entry.length){
			file.entry.length = file.position;
		}
		
		return read;
	}
	
	/**
	 * Delete the named file
	 */
	public int unlink(String name){
		FileTableEntry fte = exists(name);
		
		if(fte == null){
			return -1;
		}else if(fte.openCount > 0){
			fte.deleting = true;
			return 0;
		}else{
			// remove file from entry table and fat table
			int blockNumber = fte.firstBlock;
			
			int t;
			
			while(blockNumber != -1){
				t = fat[blockNumber];
				
				availableBlocks[blockNumber] = false;
				
				fat[blockNumber] = -2;
				
				writeDrive(Configuration.bootBlockLength  + (blockNumber*4), Lib.bytesFromInt(-2));
				
				blockNumber = t;
				break;
			}
			
			// remove entry
			for(int i = 1; i < Configuration.maxFiles; i++){
				if(files[i] != null && files[i].equals(fte)){
					
					writeDrive(Configuration.bootBlockLength + Configuration.fatLength + (i * Configuration.fileEntrySize), new byte[Configuration.fileEntrySize]);
					
					files[i] = null;
					break;
				}
			}
			return 0;
		}
	}
	
	/**
	 * Set the file position to a new position
	 */
	public int seek(int fid, int position, PCB process){
		OpenFile file = process.files[fid];
		
		if(position < file.entry.length){
			file.position = position;
			return 0;
		}else{
			return -1;
		}
	}
	
	/**
	 * Read from hard drive
	 * 
	 * @param position
	 * @param data
	 * @return
	 */
	private int readDrive(int position, byte[] data){
		IOOperation operation = new IOOperation();
		operation.action = HardDrive.read;
		operation.position = position; 
		operation.length = data.length;
				
		machine.hd.operations.add(operation);
		
		waitForHardDrive();
		
		System.arraycopy(operation.rdata, 0, data, 0, operation.rdata.length);
		
		return operation.rval;
	}
	
	/**
	 * Write to hard drive
	 * 
	 * @param position
	 * @param data
	 * @return
	 */
	private int writeDrive(int position, byte[] data){
		IOOperation operation = new IOOperation();
		operation.action = HardDrive.write;
		operation.position = position; 
		operation.rdata = data;
		
		machine.hd.operations.add(operation);
		
		waitForHardDrive();
		
		return operation.rval;
	}
	
	/**
	 * Wait for the HardDrive to respond to our IO request.
	 * 
	 * @throws InterruptedException
	 */
	private void waitForHardDrive(){
		List<Interrupt> reinterrupts = new ArrayList<Interrupt>();
		
		// take interrupts till we get a response from the hard disk
		while(true){
			Interrupt interrupt;
			try {
				interrupt = machine.getInterrupts().take();
				
				if(interrupt instanceof HardDrive){
					interrupt.acknowledge();
					break;
				}else{
					reinterrupts.add(interrupt);
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// add interrupts back to queue and continue
		machine.getInterrupts().addAll(reinterrupts);
	}
	
	public int addFile(FileTableEntry of){
		for(int i = 1; i < Configuration.maxFiles; i++){
			if(files[i] == null){
				files[i] = of;
				return i;
			}
		}
		
		throw new KernelFault("Too many files open");
	}
	
	/**
	 * find a free hard drive block
	 * 
	 * @return
	 */
	private int findFreeBlock() {
		for(int i = 0; i < fat.length; i++){
			if(fat[i] == -2){
				return i;
			}
		}
		throw new KernelFault("Hard disk full!");
	}
	
	public FileTableEntry[] getFiles(){
		return files;
	}


}
