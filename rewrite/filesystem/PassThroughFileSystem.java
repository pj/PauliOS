package filesystem;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import machine.Config;

import hardware.IOOperation;
import hardware.Interrupt;

public class PassThroughFileSystem extends Interrupt {
	/**
	 * Constants for action types
	 */
	public static final int open = 0;
	public static final int create = 1;
	public static final int read = 2;
	public static final int write = 3;
	public static final int close = 4;
	
	/**
	 * Constants for error numbers
	 */
	public static final int non_existant = -1;
	
	// queue of IO Operations
	public LinkedBlockingQueue<IOOperation> operations = new LinkedBlockingQueue<IOOperation>();
	
	public IOOperation operation;
	
	private File directory;
	
	public PassThroughFileSystem() {
		this.directory = new File(Config.getString("Machine.fs_directory"));
	}
	
	@Override
	public void run() {
		while(true){
			try {
				operation = operations.take();
				
				switch(operation.action){
				case open:
					open();
					break;
				case create:
					create();
					break;
				case read:
					read();
					break;
				case write:
					write();
					break;
				case close:
					close();
					break;					
				}
				
				// delay
				Thread.currentThread().sleep(Config.getInteger("Machine.fs_delay"));
				
				// interrupt
				interrupt();
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void close() {
		operation.rval = 1;
	}

	private void write() {
		// TODO Auto-generated method stub
		
	}

	private void read() {
		// TODO Auto-generated method stub
		
	}

	private void create() {
		if(!checkName(operation.name)){
			operation.rval = -1;
			return;
		}
		
		operation.file = new File(operation.name);
		operation.rval = 0;
		
		if(operation.file.exists()){
			operation.file = null;
			operation.rval = -1;
		}
		
	}

	private void open() {
		if(!checkName(operation.name)){
			operation.rval = -1;
			return;
		}
		
		operation.file = new File(operation.name);
		operation.rval = 0;
		
		if(!operation.file.exists()){
			operation.file = null;
			operation.rval = -1;
		}
	}
	
	private static boolean checkName(String name) {
		char[] chars = name.toCharArray();

		for (int i = 0; i < chars.length; i++) {
			if (chars[i] < 0 || chars[i] >= allowedFileNameCharacters.length)
				return false;
			if (!allowedFileNameCharacters[(int) chars[i]])
				return false;
		}
		return true;
	}

	private static boolean[] allowedFileNameCharacters = new boolean[0x80];

	private static void reject(char c) {
		allowedFileNameCharacters[c] = false;
	}

	private static void allow(char c) {
		allowedFileNameCharacters[c] = true;
	}

	private static void reject(char first, char last) {
		for (char c = first; c <= last; c++)
			allowedFileNameCharacters[c] = false;
	}

	private static void allow(char first, char last) {
		for (char c = first; c <= last; c++)
			allowedFileNameCharacters[c] = true;
	}

	static {
		reject((char) 0x00, (char) 0x7F);

		allow('A', 'Z');
		allow('a', 'z');
		allow('0', '9');

		allow('-');
		allow('_');
		allow('.');
		allow(',');
	}

}
