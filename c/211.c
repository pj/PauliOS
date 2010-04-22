#include "ag.h"	    

int t1, t2;

	int i, j;
  char c, buf[80];
  int array[64];



  case 21:
    if (pid==0) {
      restart();
      p(0);
      for (i=0; i<1000; i++);
      agireq(-1,1,0,0);
      Done();
    }
    else {
      agireq(-1,2,0,0);
      v(0);
      exit(0);
    }
    break;

  case 22:
    if (pid==0) {
      for (i=0; i<64; i++) {
	array[i] = restart();
	p(0);
      }

      for (i=0; i<64; i++) {
	for (j=0; j<64; j++) {
	  ASSERT(i==j || array[i]!=array[j]);
	}
      }
      
      Done();
    }
    else {
      v(0);
      exit(0);
    }
    break;

  case 23:
    if (pid==0) {
      restart();
      restart();
      p(0);
      exit(0);
    }
    else if (pid==1) {
      v(0);
      for (i=0; i<1024; i++);
      v(1);
      exit(0);
    }
    else {
      p(1);
      for (i=0; i<1024; i++);
      Done();
    }
    break;

  case 24:
    if (pid==0) {
      i = restart();

      ASSERT(join(i)==AUTH2);
      Done();
    }
    else {
      exit(AUTH2);
    }
    break;

  case 25:
    ASSERT(exec("alksjdf;laksjd;flkajsdf", 0, NULL) == -1);
    Done();
    break;

  case 27:
    switch (pid) {
    case 0:
      restart();
      p(0);
      ASSERT(join(lw(0)) != AUTH2);
      Done();
      break;
    case 1:
      restart();
      sw(0,restart());
      v(0);
      break;
    case 2:
      break;
    case 3:
      exit(AUTH2);
    default:
      ASSERTNOTREACHED();
    }
    break;

  case 28:
    if (pid==0) {
      array[0] = (int) file1;
      array[1] = (int) file2;
      ASSERT(exec(name, 2, (char**) array)!=-1);
    }
    else {
      ASSERT(argc == 2 &&
	     strcmp(argv[0], file1)==0 &&
	     strcmp(argv[1], file2)==0);
      Done();
    }
    break;

  case 29:
    if (pid==0) {
      for (i=0; i<8; i++)
	array[i] = (int) bigstring;
      exec(name, 8, (char**) array);
    }
    else {
      ASSERT(argc == 8);
      for (i=0; i<8; i++) {
	ASSERT(strcmp(argv[i], bigstring)==0);
      }
      Done();
    }
    break;

  case 30:
    if (pid==0) {
      for (i=0; i<16; i++) {
	array[i] = (int) reallybigstring;
      }
      ASSERT(exec(name, 16, (char**) array)==-1);
      Done();
    }
    break;

  default:
    ASSERTNOTREACHED();
  }
    
  return 0;
}
