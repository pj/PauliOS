package kernel;

/**
 * Error thrown when something unrecoverable happens in the kernel.
 * @author pauljohnson
 *
 */
public class KernelFault extends RuntimeException {
	private String message;
	
	public KernelFault(String message) {
		this.message = message;
	}
	
	
	@Override
	public String getMessage() {
		return message;
	}
}
