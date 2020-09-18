package de.srsoftware.web4rail.moving;

import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class Train {
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	private Vector<Car> cars = new Vector<Car>();
	private String name = null;
	private Block block = null;
	private Route route;
	
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

	public void block(Block block) {
		this.block = block;
	}

	public String start() {
		if (block == null) return t("{] not in a block",this); 
		HashSet<Route> routes = block.routes();
		Vector<Route> availableRoutes = new Vector<Route>();
		for (Route route : routes) {
			Vector<Tile> path = route.path();
			if (path.firstElement() != block) continue;
			if (route.inUse()) continue;
			availableRoutes.add(route);
		}
		Random rand = new Random();
		if (route != null) route.unlock();
		int sel = rand.nextInt(availableRoutes.size());
		route = availableRoutes.get(sel).lock(this);
		return t("started {}",this); 
	}
}
