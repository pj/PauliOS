#include "ag.h"

#define semSleep		0
#define semMiniWait		1

#define valMiniSleep		0

void run(int argc, char *argv[]) {
    int sleep = agLoad(valMiniSleep);

    parentSignal();

    if (sleep)
	agP(semSleep);
    else
	agP(semMiniWait);

    exit(0);
}
