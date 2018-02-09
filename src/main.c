#include<stdio.h>

int zaehlen() {
	static int counter = 0;
	return ++counter;
}

int main() {
	printf("Zähler: %d\n", zaehlen());
	printf("Zähler: %d\n", zaehlen());
	printf("Zähler: %d\n", zaehlen());
	return 0;
}
