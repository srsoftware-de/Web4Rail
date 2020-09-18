package de.srsoftware.web4rail.moving;

import java.util.Vector;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Window;

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

	public Tag props() {
		Window window = new Window("train-properties",t("Properties of {}",getClass().getSimpleName()));
		
		return window;
	}
	
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
}
