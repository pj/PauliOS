#ifndef AG_H
#define AG_H

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int ag1(int a0);
int ag2(int a0, int a1);
int ag3(int a0, int a1, int a2);
int ag4(int a0, int a1, int a2, int a3);

__asm__("
ag1:
ag2:
ag3:
ag4:
	addiu	$2,$0,-1	; \
	syscall			; \
	j	$31		; \
");

#define	reqP			15
#define	reqV			14
#define	reqSW			13
#define reqLW			12
#define reqDone			11
#define reqFail			10

#define semProcessLock		15
#define semChildWait		14

inline void agP(int semaphoreID) {
    ag2(reqP, semaphoreID);
}

inline void agV(int semaphoreID) {
    ag2(reqV, semaphoreID);
}

inline void waitChild() {
    agP(semChildWait);
}

inline void signalParent() {
    agV(semChildWait);
}

#define valTestID		15
#define valProcessCount		14
#define valShellProgramNameBase	10
#define valRandom		9

inline void agStore(int index, int value) {
    ag3(reqSW, index, value);
}

inline int agLoad(int index) {
    return ag2(reqLW, index);
}

inline void agDone() {
    ag1(reqDone);
}

inline void agFail() {
    ag1(reqFail);
}

void getStringArgument(char *dst, int startIndex, int numWords) {
    int i;
    for (i=0; i<numWords; i++)
	*(int *) &dst[i*4] = agLoad(startIndex+i);
}

int processID, testID;
char shellProgramName[16];

int restart() {
    return exec(shellProgramName, 0, null);
}

void run(int argc, char *argv[]);

int main(int argc, char *argv[]) {
    /** if this is the first process... */
    if (agLoad(valProcessCount) == 0) {
	/** initialize mutex */
	agV(semProcessLock);
    }

    /** acquire process lock */
    agP(semProcessLock);

    processID = agLoad(valProcessCount);
    agStore(valProcessCount, processID+1);

    /** release process lock */
    agV(semProcessLock);

    getStringArgument(shellProgramName, valShellProgramNameBase, 4);

    testID = agLoad(valTestID);

    run(argc, argv);
    return 0;
}

#endif
