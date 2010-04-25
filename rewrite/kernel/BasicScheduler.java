package kernel;

import machine.Configuration;

/**
 * Basic round robin Scheduler
 * @author pauljohnson
 *
 */
public class BasicScheduler implements Scheduler {

	private PCB[] processes = new PCB[Configuration.maxProcesses];
	
	private int next = 0;
	
	@Override
	public PCB schedule(PCB currentProcess) {

		int first = next;
		
		for(int i = 0; i < Configuration.maxProcesses ; i++){
			next = (first + i) % Configuration.maxProcesses;
			PCB process = processes[next];
			
			if(process != null && process.state == PCB.ready){
				next++;
				return process;
			}
		}
		 
		return null;
	}

	@Override
	public void addProcess(PCB pcb) {
		// TODO Auto-generated method stub
		processes[pcb.pid] = pcb;
	}
	
	@Override
	public void removeProcess(PCB pcb){
		processes[pcb.pid] = null;
	}

}
