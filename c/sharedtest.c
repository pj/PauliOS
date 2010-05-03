#include "stdio.h"
#include "stdlib.h"

int shared_variable = 0;

int main(int argc, char** argv)
{
	int pid;
	int * statusPointer;
	int i; // not shared!
	
	pid = fork();
	
	printf("PID %d\n", pid);
	
	if(pid==0){
		// in child process
		printf("In child Process\n");
		for(i =0; i < 10; i++){
			shared_variable++;
			printf("Child process shared_variable: %d\n", shared_variable);
		}
	}else{
		// in parent process
		printf("In parent process child pid: %d\n", pid);
		for(i =0; i < 10; i++){
			shared_variable++;
			printf("Parent process shared_variable: %d\n", shared_variable);
		}
	}
	
	// make sure we don't exit before child process has completed
	join(pid, statusPointer);
}
