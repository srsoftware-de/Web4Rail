#include<stdio.h>
#define DEBUG

int main() {
	int a=2, b=3, ergebnis;
	ergebnis = (2*a) + (2*b);
	#ifdef DEBUG
	printf("* Debug: ergebnis = (2*%d) + (2*%d);\n", a, b);
	#else
	printf("Das Ergebnis ist %d\n", ergebnis);
	#endif
	return 0;
}
