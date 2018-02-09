#include<stdio.h>

int main() {
	printf("Details zur Kompilierung\n\n");
	printf("Datum: %s\n", __DATE__);
	printf("Zeit: %s\n", __TIME__);
	printf("Zeile: %d\n", __LINE__);
	printf("Datei: %s\n", __FILE__);
	return 0;
}
