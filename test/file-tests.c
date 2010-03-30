#include "ag.h"

#define reqExists		0

int exists() {
    return ag1(reqExists);
}

void checkValue(int value) {
    assert(value == agLoad(valRandom));
}

#define valFile1		0
#define valFile2		2
#define valFileDescriptor	4

void run(int argc, char *argv[]) {
    int src, dst;
    int i, value;
    char file1[8], file2[8];
    
    getStringArgument(file1, valFile1, 2);
    getStringArgument(file2, valFile2, 2);

    switch (testID) {
    case 0:
	/* check creat() */
	assert(!exists());
	
	dst = creat(file2);	
	assert(dst != -1);
	
	assert(exists());
	
	agDone();
	break;
    
    case 1:
	/* check creat(), close(), and unlink() */
	assert(!exists());
	
	dst = creat(file2);
	assert(dst != -1);

	assert(exists());	
	assert(close(dst) == 0);

	assert(unlink(file2) == 0);
	assert(!exists());
	
	agDone();
	break;
    
    case 2:
	/* make sure close() really closes the file */
	for (i=0; i<17; i++) {
	    dst = creat(file2);
	    assert(dst != -1);

	    assert(exists());
	    assert(close(dst) == 0);

	    assert(unlink(file2) == 0);
	    assert(!exists());
	}
	agDone();
	break;

    case 3:
	/* make sure open eventually runs out of descriptors and that things don't die */
	for (i=0; i<10; i++) {
	    assert(open(file1) != -1);
	}
	for (i=0; i<10; i++) {
	    if (open(file1) == -1)
		agDone();
	}
	agFail();
	break;

    case 4:
	/* make sure all open files closed on process termination */
	if (processID == 0) {
	    for (i=0; i<5; i++) {
		assert(restart() != -1);
		waitChild();
	    }
	    agDone();
	}
	else {
	    for (i=0; i<5; i++) {
		assert(open(file1) != -1);
	    }
	    signalParent();
	    exit(0);
	}	
	break;
    
    case 5:
	/* check read */
	src = open(file1);
	assert(src != -1);

	assert(read(src, &value, 4) == 4);
	assert(close(src) == 0);

	checkValue(value);
	agDone();
	break;
    
    case 6:
	/* check isolation of file descriptors */
	if (processID == 0) {
	    src = open(file1);
	    assert(src != -1);

	    agStore(valFileDescriptor, src);
	    restart();
	    waitChild();

	    assert(read(src, &value, 4) == 4);
	    assert(close(src) == 0);

	    checkValue(value);
	    agDone();
	}
	else {
	    src = agLoad(valFileDescriptor);
	    assert(close(src) == -1);
	    signalParent();
	    exit(0);
	}
	break;
    
    case 7:
	/* check write */
	src = open(file1);
	assert(src != -1);
	
	dst = creat(file2);
	assert(dst != -1);

	assert(read(src, &value, 4) == 4);
	assert(write(dst, &value, 4) == 4);
	assert(close(src) == 0);
	assert(close(dst) == 0);

	dst = open(file2);
	assert(dst != -1);

	assert(read(dst, &value, 4) == 4);
	assert(close(dst) == 0);
	
	checkValue(value);
	agDone();
	break;
    
    case 8:
	/* make sure write fails peacefully on bad address and length */
	dst = creat(file2);
	assert(dst != -1);

	assert(write(dst, (void*) 0x7FFE1234, 0x7FFFFFFF) <= 0);
	    
	agDone();
	break;

    case 9:
	/* make sure read fails peacefully on read-only address */
	src = open(file1);
	assert(src != -1);

	assert(read(src, (void*) 0x00000000, 4) <= 0);

	agDone();
	break;

    case 10:
	/* make sure standard input uses the console */
	for (i=0; i<4; i++)
	    assert(read(fdStandardInput, &((char*) &value)[i], 1) == 1);

	checkValue(value);
	agDone();
	break;

    case 11:
	/* make sure standard output does not interleave */
	if (processID == 0) {
	    restart();

	    i = strlen(file1);
	    waitChild();
	    
	    write(fdStandardOutput, file1, i);
	    waitChild();

	    agDone();
	}
	else {
	    i = strlen(file1);
	    signalParent();
	    
	    write(fdStandardOutput, file1, i);
	    signalParent();
	}
	break;
    }

    assertNotReached();
}
