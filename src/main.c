#include<stdio.h>
#include<string.h>

int main() {
	struct person {
		char name[50];
		int alter;
	}
	kurt = { "Kurt Kanns", 33 },
	antonia = { "Antonia", 23 };

	struct person dieter = { "Dieter Dunkel", 27 };

	printf("kurt, Name: %s, Alter: %d\n", kurt.name, kurt.alter);
	printf("antonia, Name: %s, Alter: %d\n", antonia.name, antonia.alter);
	printf("dieter, Name: %s, Alter: %d\n", dieter.name, dieter.alter);
	return 0;
}
