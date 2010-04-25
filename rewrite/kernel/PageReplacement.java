package kernel;

import machine.Page;

/**
 * Interface for objects wishing to implement page replacement
 * 
 * @author pauljohnson
 *
 */
public interface PageReplacement {
	public void replace(Page pageToLoad);
	
	public Page getReplacedPage();
	
	public int getPhysicalPageNumber();
	
	public void addProcess(PCB pcb);
	
	public void removeProcess(PCB pcb);

}
