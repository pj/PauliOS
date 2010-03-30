package hardware;

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			interrupt();
		}
	}

}
