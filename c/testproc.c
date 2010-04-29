#include "syscall.h"

#define BUF_SIZE 100

int main(int argc, char** argv)
{
	
	int pid;
	int *statusPointer;
	int joinReturn;
	
	pid = exec("testwrite.coff", 0, 0);
	
	printf("pid: %d\n", pid);
	
	joinReturn = join(pid, statusPointer);
	
	if(joinReturn == 1){
		printf("join successful return status: %d\n", *statusPointer);
	}else if(joinReturn == 0){
		printf("unhandled exception return status: %d\n", *statusPointer);
	}else{
		printf("already exited or not a known pid\n");
	}
}