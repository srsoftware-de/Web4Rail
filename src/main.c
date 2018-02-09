#include<stdio.h>

// Funktions-Prototypen
float eingabeZahl();
float multipliziere(float zahl1, float zahl2);
void ausgabeErgebnis(float ergebnis);

// Hauptprogramm
int main() {
	// Rechenvorgang
	ausgabeErgebnis(multipliziere(eingabeZahl(), eingabeZahl()));
	return 0;
}

// Funktionen
float eingabeZahl() {
	float eingabe;
	printf("\nEingabe Zahl: ");
	scanf("%f", &eingabe);
	return eingabe;
}

float multipliziere(float zahl1, float zahl2) {
	return (zahl1 * zahl2);
}

void ausgabeErgebnis(float ergebnis) {
	printf("\nErgebnis: %f\n", ergebnis);
}
