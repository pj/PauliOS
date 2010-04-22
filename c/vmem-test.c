#include "ag.h"

#define semSleep		0
#define semMiniWait		1

#define valMiniSleep		0

int buf[256];

/** readspace/writespace tests */
void run(int argc, char *argv[]) {
    /* make sure virtual addresses != physical addresses */
    if (processID == 0) {
	agStore(valMiniSleep, false);	
	exec("mini", 0, null);
	childWait();

	agStore(valMiniSleep, true);
	exec("mini", 0, null);
	childWait();

	agV(semMiniWait);

	restart();
	
	agP(semSleep);
    }
    else {
	int i;

	switch (testID) {
	case 0:
	case 1:
	case 2:
	case 3:
	    for (i=0; i<256; i++)
		buf[i] = (int) &buf[i];
	    checkBuffer(buf);
	    agDone();
	    break;

	case 4:
	    fillBuffer(buf);
	    for (i=0; i<256; i++)
		assert(buf[i] == (int) &buf[i]);
	    agDone();
	    break;

	case 5:
	    fillBuffer(buf);
	    break;
	}
    }
}
