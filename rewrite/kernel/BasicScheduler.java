package kernel;

import machine.Configuration;

public class BasicScheduler implements Scheduler {

	private PCB[] processes;
	
	private int next = 0;
	
	@Override
	public PCB schedule() {

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
	public void setProcesses(PCB[] processes) {
		this.processes = processes;
	}

}
