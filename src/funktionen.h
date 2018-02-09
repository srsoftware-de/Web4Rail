#ifndef FUNKTIONEN_H

#define FUNKTIONEN_H

#include<time.h>


int mult(int a, int b) {
	return a * b;
}

int add(int a, int b) {
	return a + b;
}

void printTime(){
	time_t now;
	time(&now);
	struct tm *myTm;
	myTm = localtime(&now);
	printf("%02d:%02d:%02d Uhr\n", myTm->tm_hour, myTm->tm_min, myTm->tm_sec);
}

long hoch2(int n) {
	return n * n;
}

long hoch3(int n) {
	return n * n * n;
}

#endif
