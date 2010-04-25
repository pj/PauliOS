package kernel;

import machine.Configuration;
import machine.Page;

public class BasicPageReplacement implements PageReplacement {

	private PCB[] processes;
	
	private int next = 0;
	
	private boolean[] inUse;
	
	private Page replacementPage;
	private int physicalPageNumber;
	
	public BasicPageReplacement() {
		inUse = new boolean[Configuration.numPhysPages];
		
		for(int i = 0; i < Configuration.numPhysPages; i++){
			inUse[i] = false;
		}
	}
	
	@Override
	public void replace() {
		for(int i = 0; i < Configuration.numPhysPages; i++){
			if(!inUse[i]){
				physicalPageNumber = i;
				inUse[i] = true;
				return;
			}
		}
	}

	@Override
	public int getPhysicalPageNumber() {
		return physicalPageNumber;
	}

	@Override
	public Page getReplacedPage() {
		return replacementPage;
	}
	
	@Override
	public void addProcess(PCB pcb) {
		// TODO Auto-generated method stub
		processes[pcb.pid] = pcb;
	}

}
