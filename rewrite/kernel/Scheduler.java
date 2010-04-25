package kernel;

/**
 * Interface for an object that wishes to schedule processes
 * 
 * @author pauljohnson
 *
 */
public interface Scheduler {

	PCB schedule(PCB currentProcess);

	void addProcess(PCB pcb);

}
