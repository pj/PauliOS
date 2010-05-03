package filesystem;

import java.util.Arrays;
import machine.Configuration;
import machine.Machine;
import junit.framework.TestCase;
import kernel.Kernel;
import kernel.PCB;

public class FileSystemTest extends TestCase {

	Machine machine;
	
	BasicFileSystem fs;
	
	@Override
	protected void setUp() throws Exception {
		CreateFS.main(null);
		
		machine = new Machine();
		machine.createDevices();
		
		// start hd thread
		new Thread(machine.hd, "Hard Drive Thread").start();
		
		fs = new BasicFileSystem();
		fs.initialize(machine);
		
		Configuration.driveDelay = 0;
		
		super.setUp();
	}
	
	public void testInitialize(){
		
	}
	
	public void testOpen()  throws Exception {
		PCB process = new PCB();
		
		int fid = fs.open("sh.coff", process);
		
		assertTrue(fid == 2);
		
		assertTrue(process.files[2].name.equals("sh.coff"));
	}
	
	public void testClose() throws Exception {
		PCB process = new PCB();
		
		int fid = fs.open("sh.coff", process);
		
		
		int rval = fs.close(fid, process);
		
		assertTrue(rval == 0);
		
		rval = fs.close(5, process);
		
		assertTrue(rval == -1);
	}
	
	public void testCreate() throws Exception {

		
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		assertTrue(fid != -1);
		
		int rval = fs.close(fid, process);
		
		assertTrue(rval == 0);
		
		fid = fs.open("blah", process);
		
		assertTrue(fid != -1);
	}
	
	public void testUnlink() throws Exception{
		PCB process = new PCB();
		
		int rval = fs.unlink("sh.coff");

		assertTrue(rval == 0);
		
		int fid = fs.open("sh.coff", process);
		
		assertTrue(fid == -1);
		
		fid = fs.create("sh.coff", process);
		
		assertTrue(fid != -1);
		
		rval = fs.close(fid, process);
		
		assertTrue(rval == 0);
		
		fid = fs.open("sh.coff", process);
		
		assertTrue(fid != -1);
	}
	
	public void testWrite() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
	}
	
	public void testWriteMultiblock() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data2 = new byte[3000];
		
		Arrays.fill(data2, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data2.length; i++){
			machine.memory().writeMem(i, 1, data2[i]);
		}
					
		int rval = fs.write(fid, data2.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data2.length);
	}
	
	public void testWriteOffset() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		fs.write(fid, 1000, 0, new Kernel(machine), process);
		
		fs.seek(fid, 500, process);
		
		byte[] data2 = new byte[200];
		
		Arrays.fill(data2, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data2.length; i++){
			machine.memory().writeMem(i, 1, data2[i]);
		}
					
		int rval = fs.write(fid, data2.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data2.length);
	}
	
	public void testWriteMultiOffset() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		fs.write(fid, 3000, 0, new Kernel(machine), process);
		
		fs.seek(fid, 500, process);
		
		byte[] data2 = new byte[2000];
		
		Arrays.fill(data2, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data2.length; i++){
			machine.memory().writeMem(i, 1, data2[i]);
		}
					
		int rval = fs.write(fid, data2.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data2.length);
	}
	
	public void testReadWrite() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 0, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testReadWriteMultiBlock() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data = new byte[2000];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 0, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		for(int x = 0; x < 2000; x++){
			if(out[x] != data[x]){
				System.out.println(out[x] + " " + data[x] + " " + x);
			}
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testReadWriteOffset() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		fs.write(fid, 3000, 0, new Kernel(machine), process);
		
		byte[] data = new byte[200];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		fs.seek(fid, 200, process);
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 200, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testReadWriteOffsetMultiBlock() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		fs.write(fid, 3000, 0, new Kernel(machine), process);
		
		byte[] data = new byte[2000];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		fs.seek(fid, 200, process);
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 200, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testWriteBeyondLength() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		fs.seek(fid, 3000, process);
		
		byte[] data = new byte[2000];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 3000, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testWriteBoundary() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data = new byte[3072];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 0, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
	
	public void testWriteBeyondLengthBoundary() throws Exception{
		PCB process = new PCB();
		
		int fid = fs.create("blah", process);
		
		byte[] data = new byte[1024];
		
		Arrays.fill(data, (byte)32);
		
		// write data to memory
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, data[i]);
		}
		
		int rval = fs.write(fid, data.length, 0, new Kernel(machine), process);
		
		assertTrue(rval == data.length);
		
		fs.seek(fid, 3072, process);
		
		for(int i = 0; i < data.length; i++){
			machine.memory().writeMem(i, 1, 17);
		}
		
		fs.write(fid, 1024, 0, new Kernel(machine), process);
		
		byte[] out = new byte[data.length];
		
		fs.seek(fid, 0, process);
		
		rval = fs.read(fid, out.length, 0, new Kernel(machine), process);
		
		for(int i =0; i < out.length; i++){
			out[i] = (byte) machine.memory().readMem(i, 1);
		}
		
		assertTrue(Arrays.equals(data, out));
	}
}
