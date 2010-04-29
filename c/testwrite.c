#include "syscall.h"

#define BUF_SIZE 100

int main(int argc, char** argv)
{
	int i;
	char buffer[BUF_SIZE];
	
	int fid = open("asdf");
	
	int count = read(fid, buffer, BUF_SIZE);
	
	write(1, buffer, count);
	
	// waste time
	for(i = 0; i < 900000000; i++){
		i++;
		i--;
	}
	
	exit(0);
}