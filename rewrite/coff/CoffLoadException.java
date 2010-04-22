package coff;

public class CoffLoadException extends Exception{
	
	private String message;
	
	public CoffLoadException(String message) {
		this.message = message;
	}
	
	
	@Override
	public String getMessage() {
		return message;
	}
}
