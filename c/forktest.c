#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
	int pid;
	
	pid = fork();
	
	if(pid==0){
		// in child process
		printf("In child Process");
	}else{
		// in parent process
		printf("In parent process child pid: %d", pid);
	}
}
