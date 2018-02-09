#include <stdio.h>
int main(){

	char c;
	printf("Mit welchem Buchstaben beginnt ihr Vorname? ");
	c = getchar();
	printf("\nIch weiss jetzt, dass Ihr Vorname mit '%c' beginnt.\n", c);


	return 0;
}
