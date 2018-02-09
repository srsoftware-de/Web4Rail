#include<stdio.h>
#include<string.h>

int main() {

	struct adresse {
		char name[50];
		char strasse[100];
		short hausnummer;
		long plz;
		char stadt[50];
	};

	// Variable der Struktur erstellen
	struct adresse adresseKurt;

	// Zugriff auf die Elemente
	strcpy(adresseKurt.name, "Kurt Kanns");
	strcpy(adresseKurt.strasse, "Kannichweg");
	adresseKurt.hausnummer = 23;
	adresseKurt.plz = 45678;
	strcpy(adresseKurt.stadt, "Kannstadt");

	printf("Name: %s\n", adresseKurt.name);
	return 0;
}
