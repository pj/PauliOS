package hardware;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import machine.Configuration;
import junit.framework.TestCase;


public class HardwareTest extends TestCase{

	public void testHardDrive() throws Exception{
		// set config options
		Configuration.diskFileName = Configuration.diskFileName;
		
		HardDrive hd = new HardDrive();
		
		LinkedBlockingQueue<IOOperation> inputQueue = new LinkedBlockingQueue<IOOperation>();
		PriorityBlockingQueue<Interrupt> interruptQueue = new PriorityBlockingQueue<Interrupt>();
		
		hd.setQueue(interruptQueue);
		
		hd.operations = inputQueue;
		
		// test data
		byte[] testData = new byte[]{1,2,3,4,5,6,7,8,9,10};
		

		
		// start hd thread
		Thread thread = new Thread(hd, "Hard Drive Thread");
		
		thread.start();
		
		// perform write operation
		IOOperation writeOp = new IOOperation();
		writeOp.action = HardDrive.write;
		writeOp.position = 0;
		writeOp.rdata = testData;
		
		inputQueue.add(writeOp);
		
		interruptQueue.take();
		
		hd.acknowledge();
		
		assertTrue(writeOp.rval == 10);
		
		// perform read
		IOOperation readOp = new IOOperation();
		readOp.action = HardDrive.read;
		readOp.position = 0;
		readOp.length = 10;
		
		inputQueue.add(readOp);
		
		interruptQueue.take();
		
		hd.acknowledge();
		
		assertTrue(Arrays.equals(testData, readOp.rdata));
		
		assertTrue(readOp.rval == 10);
		
		// test writing beyond end of file
		IOOperation writeOpBad = new IOOperation();
		writeOpBad.action = HardDrive.write;
		writeOpBad.position = 1000000000;
		writeOpBad.rdata = testData;
		
		inputQueue.add(writeOpBad);
		
		interruptQueue.take();
		
		hd.acknowledge();
		
		assertTrue(writeOpBad.rval == -1);
		
		
	}
}
