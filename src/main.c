#include<stdio.h>

// Symbolische Konstante
#define MWSTFAKTOR 0.19

int main() {
	float betrag = 350.0;
	float mwst = betrag * MWSTFAKTOR;
	printf("%.f% von %.2f Euro: %.2f Euro\n", MWSTFAKTOR * 100, betrag, mwst);
	return 0;
}
