package kernel;

public interface Scheduler {

	void setProcesses(PCB[] processes);

	PCB schedule();

}
