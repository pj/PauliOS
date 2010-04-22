package kernel;

import machine.Page;

public interface PageReplacement {
	
	public void setProcesses(PCB[] processes);
	
	public void replace();
	
	public Page getReplacedPage();
	
	public int getPhysicalPageNumber();

}
