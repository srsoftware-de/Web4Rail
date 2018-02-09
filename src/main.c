#include<stdio.h>
#include<string.h>


int main() {
	// Dateizeiger erstellen
	FILE *fp;
	int temp;

	// Datei oeffnen
	fp = fopen("Web4Rail", "r");

	if(fp == NULL) {
		printf("Datei konnte NICHT geoeffnet werden.\n");
	}else {
		// komplette Datei zeichenweise ausgeben
		while((temp = fgetc(fp))!=EOF) {
			printf("%c ", temp);
		}
		fclose(fp);
	}
}
