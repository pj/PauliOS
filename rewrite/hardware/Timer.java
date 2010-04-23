package hardware;

/**
 * Generates interrupts periodically to trigger scheduling of different processes if necessary.
 * 
 * @author pauljohnson
 *
 */
public class Timer extends Interrupt {

	private long sleepTime;
	
	public Timer(long sleepTime) {
		this.sleepTime = sleepTime;
	}
	
	@Override
	public void run() {
		while(true){
			try {
				Thread.currentThread().sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			interrupt();
		}
	}

}
