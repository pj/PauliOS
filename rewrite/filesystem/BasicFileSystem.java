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
	 * File allocation table forms a linked list of the blocks that a file contains
	 */
	public int[] fat;
	
	/**
	 * Entries of the root directory of the file system.
	 * 
	 * Note that this isn't the same as the processes files array which is the list of files
	 * that a process has open
	 * 
	 */
	public FileTableEntry[] files;

	private Machine machine;

	/**
	 * Loads the file system from the hard drive and creates the necessary data structures 
	 * to support file system operation.
	 */
	public void initialize(Machine machine){
		this.machine = machine;
		
		files = new FileTableEntry[Configuration.maxFiles];
		fat = new int[Configuration.numberOfBlocks];
		
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
		
		// create entries for files table
		for(int i = 0; i < entryCount; i++){
			System.arraycopy(fileEntries, i*Configuration.fileEntrySize, entryData, 0, 32);
			files[i] = new FileTableEntry(entryData);
		}
	}
	
	/**
	 * Checks whether the file named "name" exists on the file system.
	 * 
	 * @param name file name to check
	 * @return the FileTableEntry with the name given or null if it doesn't exist
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
	 * Gets the first available index in a processes files and returns it as the file id
	 * 
	 * @param files2
	 * @return new file id or -1 if table is full
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
	 * 
	 * @return the file id of the open file
	 */
	public int open(String name, PCB process){
		// check if file exists
		FileTableEntry entry = exists(name);
		
		if(entry == null || entry.deleting){
			return -1;
		}
		
		int fid = getFid(process.files);
		
		process.files[fid] = new OpenFile(name, entry);
		
		entry.openCount++;
		
		return fid;
	}
	
	/**
	 * close a file returning 0 if successful and -1 if unsuccessful
	 */
	public int close(int fid, PCB process){	
		// check that fid exists
		OpenFile entry = process.files[fid];

		if(entry == null){
			return -1;
		}
		
		entry.entry.openCount--;
		
		process.files[fid] = null;
		
		// delete if unlink has set deleting on the entry
		if(entry.entry.openCount == 0 && entry.entry.deleting){
			return deleteFile(entry.entry);
		}else{
			return 0;
		}
	}
	
	/**
	 * Create a file if it doesn't exist or open it if it already exists
	 * 
	 * @return new fid for the open file
	 */
	public int create(String name, PCB process){
		FileTableEntry entry = exists(name);
		
		int fid = getFid(process.files);
		
		// already exists return fid
		if(entry != null){
			entry.openCount++;
			
			process.files[fid] = new OpenFile(entry.name, entry);
			
			return fid;
		}
				
		entry = new FileTableEntry(name, -1, 0);
		
		addFile(entry);
		
		if(entry.deleting){
			return -1;
		}
		
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


	/**
	 * Read from a file to the memory of the process
	 * 
	 * @param fid id of the file to read from
	 * @param length number of bytes to read
	 * @param memoryPointer address in memory to read into
	 * @param kernel the kernel
	 * @param process to get OpenFile from
	 * @return how many bytes were read or -1 if there was an error, note might be less than length
	 * 			if end of file has been reached.
	 * 
	 */
	public int read(int fid, int length, int memoryPointer, Kernel kernel, PCB process) {
		if(fid > Configuration.maxFiles || fid < 0){
			return -1;
		}
		
		// check if the file exists
		OpenFile file = process.files[fid];
		
		if(file == null){
			return -1;
		}
		
		// are we trying to read data we haven't written
		if(file.position > file.entry.length){
			return -1;
		}
		
		int startPosition = file.position;
		int endPosition = file.position + length;
		
		if(endPosition > file.entry.length){
			endPosition = file.entry.length;
		}
		
		// find first block to read
		int block = getFilePositionBlock(file);
		
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
			kernel.checkInMemory(memoryPointer);
			
			// copy read data into memory
			for(int i = 0; i < data.length; i++){
				try {
					machine.memory().writeMem(memoryPointer++, 1, data[i]);
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
	
	/**
	 * Write from memory to a file
	 * 
	 * @param fid id of the file to write to
	 * @param length number of bytes to write
	 * @param memoryPointer address in memory to write from
	 * @param kernel the kernel
	 * @param process to get OpenFile from
	 * @return how many bytes were written or -1 if there was an error
	 * 
	 */
	public int write(int fid, int length, int memoryPointer, Kernel kernel, PCB process) {
	
		if(fid > Configuration.maxFiles || fid < 0){
			return -1;
		}
		
		OpenFile file = process.files[fid];
		
		if(file == null){
			return -1;
		}
		
		// if file position is greater than file length we need to add blocks and write 0's to them 
		if(file.position >= file.entry.length){
			// find the last block in the file
			int lastBlock = file.entry.firstBlock;
			int numBlocks = 0;
			
			while(fat[lastBlock] != -1){
				numBlocks++;
				lastBlock = fat[lastBlock];
			}
			
			// write 0's to the end of the current last block
			
//			int lastBlockOffset = file.entry.length % Configuration.blockSize;
//				
//			writeDrive( Configuration.fileOffset + (lastBlock*Configuration.blockSize) + lastBlockOffset, new byte[Configuration.blockSize-lastBlockOffset]);
			
			
			// find pages and write 0's to them
			
			// number of blocks to add
			int addBlocks = (file.position / Configuration.blockSize) - numBlocks;
			
			for(int i=0; i < addBlocks; i++){
				int newBlock = findFreeBlock();
				
				// write 0's to drive
				writeDrive(Configuration.fileOffset + (newBlock*Configuration.blockSize), new byte[Configuration.blockSize]);
				
				fat[lastBlock] = newBlock;
				lastBlock = newBlock;
				fat[lastBlock] = -1;
			}
			
			file.entry.length = file.position;
		}
		
		int startPosition = file.position;
		int endPosition = file.position + length;
		
		byte[] data;
		
		int diskPosition;
		
		int block = getFilePositionBlock(file);
		
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
				
				// no previous block means we are setting the first block
				if(prevBlock == -1){
					fat[file.entry.firstBlock] = newBlock;
					fat[newBlock] = -1;
				}else{
					fat[prevBlock] = newBlock;
					fat[newBlock] = -1;
				}

				
				// write changes in fat to disk
				writeDrive(Configuration.bootBlockLength + (prevBlock * 4), Lib.bytesFromInt(newBlock));
				writeDrive(Configuration.bootBlockLength + (newBlock * 4), Lib.bytesFromInt(-1));
				
				block = newBlock;
			}
			
			// read from memory to data
			data = new byte[blockLength];
			
			kernel.checkInMemory(memoryPointer);
			
			for(int i = 0; i < blockLength; i++){
				try {
					data[i] = (byte)machine.memory().readMem(memoryPointer++, 1);
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
	 * Get block number that file.position is in
	 * 
	 * @param file to get position from
	 * @return block number that contains the position
	 */
	private int getFilePositionBlock(OpenFile file) {
		int firstBlock = file.entry.firstBlock;
		
		for(int i = 1024; i <= file.entry.length; i += 1024){
			if(file.position < i){
				return firstBlock;
			}else{
				firstBlock = fat[firstBlock];
			}
		}
		return firstBlock;
	}
	
	/**
	 * Delete the named file - if it is still open by a process then mark the entry as deleting
	 * 
	 * @return 0 on success -1 on failure
	 */
	public int unlink(String name){
		FileTableEntry fte = exists(name);
		
		if(fte == null){
			return -1;
		}else if(fte.openCount > 0){
			fte.deleting = true;
			return 0;
		}else{
			return deleteFile(fte);
		}
	}

	/**
	 * Actually delete a file - can be called from close as well as unlink
	 * 
	 * @param fte entry to delete
	 * @return -1 on failure 0 on success
	 */
	private int deleteFile(FileTableEntry fte) {
		// remove file from entry table and fat table
		int blockNumber = fte.firstBlock;
		
		int t;
		
		while(blockNumber != -1){
			t = fat[blockNumber];
			
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
	
	/**
	 * Set the file position to a new position
	 */
	public void seek(int fid, int position, PCB process){
		OpenFile file = process.files[fid];
		
		file.position = position;
	}
	
	/**
	 * Read from physical drive
	 * 
	 * @param position on disk to read from
	 * @param data data buffer to read into
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
	 * @param position to write to
	 * @param data buffer to write to drive
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
	
	/**
	 * Add an entry to the files[] array.
	 * 
	 * @param of entry to add
	 * @return index into the file table that the entry has been added to
	 */
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

	@Override
	/**
	 * Change the working directory of a process to the one in path
	 * 
	 * @return 0 if path exists and is a directory, -1 if it doesn't exist or is a file
	 */
	public int chdir(String path, PCB process) {
		return -1;
	}

	@Override
	/**
	 * Create a new directory, including the "." and ".." entries.
	 * 
	 * Note: doesn't need to create intermediate directories if they don't exist.
	 * 
	 * @return 0 if directory was successfully created -1 if it wasn't.
	 */
	public int mkdir(String path, PCB process) {
		return -1;
	}

	@Override
	/**
	 * Delete a directory if it is empty i.e. it only has the "." and ".." entries in it.
	 * 
	 * @return 0 if directory was successfully deleted, -1 if it wasn't.
	 */
	public int rmdir(String path, PCB process) {
		return -1;
	}
}
