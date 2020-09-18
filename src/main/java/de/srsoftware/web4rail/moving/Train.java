package de.srsoftware.web4rail.moving;

import java.util.Vector;

public class Train {
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	private Vector<Car> cars = new Vector<Car>();
	private String name = null;
	
	public Train(Locomotive loco) {
		add(loco);
	}

	public void add(Car car) {
		if (car == null) return;
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);		
	}
	
	public int length() {
		int result = 0;		
		for (Locomotive loco : locos) result += loco.length;
		for (Car car : cars) result += car.length;
		return result;
	}

	public String name() {
		return name != null ? name : locos.firstElement().name();
	}
}
