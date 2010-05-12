#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** argv)
{
	int fid;
	int rval;
	char buffer[100];
	
	rval = chdir("test");
	
	if(rval != 0){
		printf("chdir() failed\n");
	}else{
		printf("chdir() succeeded\n");
	}
	
	fid = open("asdf");
	
	if(fid != 2){
		printf("open() failed\n");
	}else{
		printf("open() succeeded\n");
	}
	
	rval = read(fid, buffer, 100);
	
	rval = write(1, buffer, rval);
	
	rval = close(fid);
	
	rval = chdir("..");
	
	if(rval != 0){
		printf("chdir() failed\n");
	}else{
		printf("chdir() succeeded\n");
	}
	
	rval = rmdir("/test");

	if(rval == 0){
		printf("rmdir() failed - directory should not be deleted if not empty\n");
	}else{
		printf("rmdir() succeeded - directory not empty\n");
	}
	
	rval = unlink("test/asdf");
	
	if(rval != 0){
		printf("unlink() failed\n");
	}else{
		printf("unlink() succeeded\n");
	}
	
	rval = rmdir("test");

	if(rval != 0){
		printf("rmdir() failed\n");
	}else{
		printf("rmdir() succeeded\n");
	}

	rval = mkdir("blah");

	if(rval != 0){
		printf("mkdir() failed\n");
	}else{
		printf("mkdir() succeeded\n");
	}

	rval = chdir("blah");
	
	if(rval != 0){
		printf("chdir() failed\n");
	}else{
		printf("chdir() succeeded\n");
	}
	
	rval = creat("qwer");
	
	if(rval != 2){
		printf("creat() failed\n");
	}else{
		printf("creat() succeeded\n");
	}
	
	write(rval, "hello world!", 12);
	
	close(rval);
	
	rval = open("qwer");
	
	read(rval, buffer, 12);
	
	write(1, buffer, 12);
	
	rval = close(rval);
	
	unlink("qwer");
	
	rval = chdir("/");
	
	if(rval != 0){
		printf("chdir() failed\n");
	}else{
		printf("chdir() succeeded\n");
	}
	
	rval = rmdir("blah");

	if(rval != 0){
		printf("rmdir() failed\n");
	}else{
		printf("rmdir() succeeded\n");
	}

  return 0;
}