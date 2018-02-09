#include <iostream>             // Schnittstellen fr die Streams einbinden
using namespace std;

int main(void) {
	char c;                        // Variable fr ein gelesenes Zeichen
	bool spc = false;                // Variable: War das letzte Zeichen ein Space?

	while ( cin.get(c) ) {
		if ( c == ' ' ){                 // Space?
			if ( !spc ) cout << c;     // nur das jeweils erste Space ausgeben
			spc = true;                // merken, da˜ Space gelesen
		} else {
			cout << c;               // Zeichen au˜er Spaces bernehmen
			spc = false;               // merken, da˜ kein Space gelesen
		}
	}                             // Ende des Schleifen-Rumpfes
	return 0;
}                                // Ende des Hauptprogramms
