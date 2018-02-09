#include<stdio.h>

int main() {

	int zahl = 7;
	int *zeiger;
	zeiger = &zahl;
	printf("Zeiger-Adresse: %d\n", zeiger);
	printf("Zeiger-Wert: %d\n", *zeiger);

	return 0;
}
