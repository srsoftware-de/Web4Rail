#include<stdio.h>
#include "funktionen.h"

int main() {
	long (*rechne) (int);

	int zahl = 3;

	rechne = hoch2;
	printf("%d hoch 2: %d\n", zahl, rechne(zahl));

	rechne = hoch3;
	printf("%d hoch 3: %d\n", zahl, rechne(zahl));

	return 0;
}
