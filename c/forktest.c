#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
	int pid;
	int * statusPointer;
	
	pid = fork();
	
	printf("PID %d\n", pid);
	
	if(pid==0){
		// in child process
		printf("In child Process\n");
	}else{
		// in parent process
		printf("In parent process child pid: %d\n", pid);
		
		join(pid, statusPointer);
	}
	

}
