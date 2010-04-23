package coff;

/**
 * Exception thrown when an error occurs while loading a coff file into memory.
 * 
 * @author pauljohnson
 *
 */
public class CoffLoadException extends Exception{
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public CoffLoadException(String message) {
		this.message = message;
	}
	
	
	@Override
	public String getMessage() {
		return message;
	}
}
