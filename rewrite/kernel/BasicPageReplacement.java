package kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import machine.Configuration;
import machine.Page;

public class BasicPageReplacement implements PageReplacement {

	private PCB[] processes = new PCB[Configuration.maxProcesses];
	
	private Page[] clockBuffer = new Page[Configuration.numPhysPages];
	
	private int clockHand = 0;
	
	// what physical pages are in use?
	private boolean[] inUse;
	
	private Page replacementPage;
	private int physicalPageNumber;
	
	public BasicPageReplacement() {
		inUse = new boolean[Configuration.numPhysPages];
		
		Arrays.fill(inUse, false);
	}
	
	@Override
	public void replace(Page pageToLoad) {
		replacementPage = null;
		
		for(int i = 0; i < Configuration.numPhysPages; i++){
			if(!inUse[i]){
				clockBuffer[i] = pageToLoad;
				physicalPageNumber = i;
				inUse[i] = true;
				return;
			}
		}
		
		// no pages available - clock algorithm
		int first = clockHand;
		
		for(int i = 0; i < Configuration.numPhysPages; i++){
			Page page = clockBuffer[clockHand];
			
			if(page.used){
				page.used = false;
			}else{
				physicalPageNumber = clockHand;
				replacementPage = clockBuffer[clockHand];
				
				clockBuffer[clockHand] = pageToLoad;
				clockHand = (first + i) % Configuration.numPhysPages;
				break;
			}
			
			clockHand = (first + i) % Configuration.numPhysPages;
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
		processes[pcb.pid] = pcb;
	}

	/**
	 * Invalidate pages of a process that has exited
	 * 
	 */
	@Override
	public void removeProcess(PCB pcb) {
		for(Page page : pcb.pageTable){
			if(page != null && page.present){
				inUse[page.ppn] = false;
				clockBuffer[page.ppn] = null;
			}
		}
	}

}
