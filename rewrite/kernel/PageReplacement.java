package kernel;

import machine.Page;

/**
 * Interface for objects wishing to implement page replacement
 * 
 * @author pauljohnson
 *
 */
public interface PageReplacement {
	public void replace();
	
	public Page getReplacedPage();
	
	public int getPhysicalPageNumber();
	
	public void addProcess(PCB pcb);

}
