#include "ag.h"

#define valChildProcessID	0

#define ___big			"abcdefgh"
#define __big			___big ___big ___big ___big ___big ___big ___big ___big
#define _big			__big __big __big __big __big __big __big __big
#define big			_big _big _big _big _big _big _big _big

void run(int argc, char *argv[]) {
    int i, j;
    int array[16];

    switch (testID) {
    case 0:
	if (processID == 0) {
	    restart();
	    childWait();
	    for (i=0; i<1024; i++);
	    ag2(-1,1);
	    agDone();
	}
	else {
	    ag2(-1,2);
	    parentSignal();
	    exit(0);
	}
	break;

    case 1:
	if (processID == 0) {
	    for (i=0; i<16; i++) {
		array[i] = restart();
		childWait();
	    }

	    for (i=0; i<16; i++)
		for (j=0; j<16; j++)
		    assert(i==j || array[i]!=array[j]);

	    agDone();
	}
	else {
	    parentSignal();
	    exit(0);
	}
	break;

    case 2:
	if (processID == 0) {
	    restart();
	    restart();
	    childWait();
	    exit(0);
	}
	else if (processID == 1) {
	    parentSignal();
	    for (i=0; i<1024; i++);
	    parentSignal();
	    exit(0);
	}
	else {
	    childWait();
	    for (i=0; i<1024; i++);
	    agDone();
	}
	break;

    case 3:
	if (processID == 0) {
	    i = restart();
	    assert(join(i, &j) == 1);
	    assert(j == agLoad(valRandom));
	    agDone();
	}
	else {
	    exit(agLoad(valRandom));
	}
	break;

    case 4:
	assert(exec("abcdefghijklmnopqrstuvwxyz", 0, null) == -1);
	agDone();
	break;

    case 5:
	switch (processID) {
	case 0:
	    restart();
	    childWait();
	    assert(join(agLoad(valChildProcessID), &j) == 1);
	    assert(j == 0);
	    agDone();
	    break;
	case 1:
	    restart();
	    agStore(valChildProcessID, restart());
	    parentSignal();
	    break;
	case 2:
	    exit(0);
	    break;
	case 3:
	    exit(agLoad(valRandom));
	    break;
	}
	break;

    case 6:
	if (processID == 0) {
	    array[0] = (int) "this";
	    array[1] = (int) "is-dumb";
	    assert(exec(shellProgramName, 2, (char**) array) != -1);
	    exit(0);
	}
	else {
	    assert(argc == 2 &&
		   strcmp(argv[0], "this")==0 &&
		   strcmp(argv[1], "is-dumb")==0);
	    agDone();
	}
	break;

    case 7:
	for (i=0; i<8; i++)
	    array[i] = (int) big;
	assert(exec(shellProgramName, 8, (char**) array) == -1);
	break;

    case 11:
	/* make lots of child processes */
	if (processID == 0) {
	    for (i=0; i<20; i++) {
		restart();
		waitChild();
	    }

	    agDone();
	}
	else {
	    signalParent();
	    exit(0);
	}
	break;
    }

    agFail();
}
