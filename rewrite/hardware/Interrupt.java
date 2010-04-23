package hardware;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Abstract class for implementing interrupt driven hard ware.
 * 
 * @author pauljohnson
 *
 */
public abstract class Interrupt implements Runnable, Comparable<Interrupt> {
	public abstract void run();

	protected PriorityBlockingQueue<Interrupt> queue;
	
	protected int priority = 0;
	
	public void setQueue(PriorityBlockingQueue<Interrupt> queue){
		this.queue = queue;
	}
	
	public void setPriority(int priority){
		this.priority = priority;
	}
	
	public synchronized void interrupt(){
		queue.add(this);
		
		try {
			wait();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void acknowledge(){
		notify();
	}
	
	@Override
	public int compareTo(Interrupt o) {
		if(this.priority > o.priority){
			return 1;
		}else if(this.priority < o.priority){
			return -1;
		}else{
			return 0;
		}
	}
}
