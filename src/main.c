#include<stdio.h>
#include<string.h>

int main() {
	enum { kreuz=4, pik=3, karo, herz };
	printf("Kreuz: %d, Pik: %d, Karo: %d, Herz: %d\n", kreuz, pik, karo, herz);
	return 0;
}
