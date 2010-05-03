package kernel;

import java.util.Arrays;

import machine.Configuration;
import machine.Machine;
import machine.Page;
import filesystem.BasicFileSystem;
import filesystem.CreateFS;
import junit.framework.TestCase;


public class VMTest extends TestCase {

	Machine machine;
	
	BasicFileSystem fs;
	
	Kernel kernel;
	
	@Override
	protected void setUp() throws Exception {
		CreateFS.main(null);
		
		Configuration.driveDelay = 0;
		
		Configuration.numPhysPages = 1;
		
		machine = new Machine();
		machine.createDevices();
		
		// start hd thread
		new Thread(machine.hd, "Hard Drive Thread").start();
		
		fs = new BasicFileSystem();
		fs.initialize(machine);
		
		kernel = new Kernel(machine);
		
		kernel.fs = fs;
		
		kernel.scheduler = new BasicScheduler();
		kernel.pageReplacer = new BasicPageReplacement();
		
		machine.memory().vmEnabled = true;
		
		super.setUp();
	}
	
	public void testSavePage() throws Exception {
		PCB pcb = new PCB();
		pcb.pageTable = new Page[Configuration.numVirtualPages];
		pcb.name = "blah";
		kernel.process = pcb;
		kernel.addProcess(pcb);
		
		Page page = new Page(0, 0, true, false, true, true);
		
		page.pid = pcb.pid;
		
		pcb.pageTable[0] = page;
		
		
		// write some data to memory
		
		machine.memory().setPageTable(pcb.pageTable);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 32);
		}
		
		kernel.savePage(page);
		
		assertFalse(page.dirty);
		assertFalse(page.present);
		assertTrue(page.ppn == -1);
		assertTrue(page.saved);
		
		// write over memory
		
		page.ppn = 0;
		page.present = true;
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 17);
		}
		
		// read from page file back to memory
		int fid = fs.open("0_blah" , pcb);
		
		fs.read(fid, Configuration.pageSize, 0, kernel, pcb);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			assertTrue(machine.memory().readMem(i, 1) == 32);
		}
	}
	
	public void testSavePageOffset() throws Exception {
		PCB pcb = new PCB();
		pcb.pageTable = new Page[Configuration.numVirtualPages];
		pcb.name = "blah";
		kernel.process = pcb;
		kernel.addProcess(pcb);
		
		Page page = new Page(3, 0, true, false, true, true);
		
		page.pid = pcb.pid;
		
		pcb.pageTable[0] = page;
		
		
		// write some data to memory
		
		machine.memory().setPageTable(pcb.pageTable);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 32);
		}
		
		kernel.savePage(page);
		
		assertFalse(page.dirty);
		assertFalse(page.present);
		assertTrue(page.ppn == -1);
		assertTrue(page.saved);
		
		// write over memory
		
		page.ppn = 0;
		page.present = true;
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 17);
		}
		
		// read from page file back to memory
		int fid = fs.open("0_blah" , pcb);
		
		fs.seek(fid, 3072, pcb);
		
		int rval = fs.read(fid, Configuration.pageSize, 0, kernel, pcb);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			assertTrue(machine.memory().readMem(i, 1) == 32);
		}
	}
	
	public void testLoadPage() throws Exception {
		PCB pcb = new PCB();
		pcb.pageTable = new Page[Configuration.numVirtualPages];
		pcb.name = "blah";
		kernel.process = pcb;
		kernel.addProcess(pcb);
		
		Page page = new Page(0, 0, true, false, true, true);
		
		page.pid = pcb.pid;
		
		pcb.pageTable[0] = page;
		
		
		// write some data to memory
		
		machine.memory().setPageTable(pcb.pageTable);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 32);
		}
		
		int fid = fs.create("0_blah", pcb);
		
		fs.write(fid, Configuration.pageSize, 0, kernel, pcb);
		
		fs.close(fid, pcb);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i, 1, 17);
		}
		
		page.present = false;
		page.ppn = 0;
		page.saved = true;
		
		kernel.loadPage(page, 0);
		
		assertFalse(page.dirty);
		assertTrue(page.present);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			assertTrue(machine.memory().readMem(i, 1) == 32);
		}
	}
	
	public void testSaveLoad() throws Exception {
		PCB pcb = new PCB();
		pcb.pageTable = new Page[Configuration.numVirtualPages];
		pcb.name = "blah";
		kernel.process = pcb;
		kernel.addProcess(pcb);
		
		Page page = new Page(3, 0, true, false, true, true);
		
		page.pid = pcb.pid;
		
		pcb.pageTable[3] = page;
		
		
		// write some data to memory
		
		machine.memory().setPageTable(pcb.pageTable);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i + (3*Configuration.pageSize), 1, 32);
		}
		
		kernel.savePage(page);
		
		assertFalse(page.dirty);
		assertFalse(page.present);
		assertTrue(page.ppn == -1);
		assertTrue(page.saved);
		
		// write over memory
		
		page.ppn = 0;
		page.present = true;
		
		for(int i = 0; i < Configuration.pageSize; i++){
			machine.memory().writeMem(i + (3*Configuration.pageSize), 1, 17);
		}
		
		page.present = false;
		
		kernel.loadPage(page, 0);
		
		for(int i = 0; i < Configuration.pageSize; i++){
			assertTrue(machine.memory().readMem((3*Configuration.pageSize)+i, 1) == 32);
		}
	}
	
	public void testPageFault() throws Exception {
		fail("not done");
	}
	
	public void testCheckInMemory() throws Exception {
		fail("not done");
	}
}
