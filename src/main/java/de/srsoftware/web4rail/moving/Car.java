package de.srsoftware.web4rail.moving;

public class Car {
	public int length;
	private String name;
	
	public Car(String name) {
		this.name = name;
	}
	
	String name(){
		return name;
	}
}
