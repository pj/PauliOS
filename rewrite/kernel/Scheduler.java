package kernel;

/**
 * Interface for an object that wishes to schedule processes
 * 
 * @author pauljohnson
 *
 */
public interface Scheduler {

	void setProcesses(PCB[] processes);

	PCB schedule();

}
