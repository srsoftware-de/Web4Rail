#include <stdio.h>

float sum(float a, float b){
	return a+b;
}

int main(){

	float a,b;
	printf("erste Zahl eingeben:");
	scanf("%f",&a);
	printf("erste Zahl eingeben:");
	scanf("%f",&b);
	printf("die Summe aus %f und %f ist %.3f",a,b,sum(a,b));
	return 0;
}
