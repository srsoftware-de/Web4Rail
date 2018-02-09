#include<stdio.h>
#include<stdlib.h>

int main() {
	int size=0;
	int *array;

	printf("Array-Groesse eingeben: ");
	scanf("%d", &size);

	// Speicher reservieren
	array = (int *) calloc(size, sizeof(int));

	if(array != NULL) {
		printf("\nSpeicher ist reserviert\n");
		free(array);
		printf("\nSpeicher wurde wieder freigegeben\n");
	}else {
		printf("\nKein freier Speicher vorhanden.\n");
	}


	return 0;
}
