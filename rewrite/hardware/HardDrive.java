package hardware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.LinkedBlockingQueue;

import machine.Configuration;


public class HardDrive extends Interrupt{
	// hard disk commands
	public final static int read = 1; // read block of data
	public final static int write = 2; // write block of data
	
	// queue of IO Operations
	public LinkedBlockingQueue<IOOperation> operations = new LinkedBlockingQueue<IOOperation>();
	
	
	private RandomAccessFile raf;
	
	public HardDrive() throws FileNotFoundException {
		raf = new RandomAccessFile(new File(Configuration.diskFileName), "rw");
	}
	

	@Override
	public void run() {
		while(true){
			IOOperation operation;
			try {
				operation = operations.take();
				try {					
					if(operation.position < 0 || operation.position > Configuration.blockSize * Configuration.numberOfBlocks){
						operation.rval = -1;
					}else{
						switch(operation.action){
						case(read):
							raf.seek(operation.position);
							operation.rdata = new byte[operation.length];
	
							operation.rval = raf.read(operation.rdata);
	
							break;
						case(write):
							raf.seek(operation.position);
							raf.write(operation.rdata);
							operation.rval = operation.rdata.length;
							break;
						}
					}
				}catch(Exception e){
					e.printStackTrace();
					operation.rval = -1;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			// delay
			try{
				Thread.currentThread().sleep(Configuration.driveDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// interrupt
			interrupt();

		}
	}
}
