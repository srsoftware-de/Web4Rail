package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Signal;

public class Train {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	private Vector<Car> cars = new Vector<Car>();
	private String name = null;
	private Block block = null;
	public Route route;	
	public int speed = 0;
	private Direction direction;
	
	public Train(Locomotive loco) {
		add(loco);
	}

	public void add(Car car) {
		if (car == null) return;
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);		
	}
	
	public void block(Block block) {
		this.block = block;
	}

	public int length() {
		int result = 0;		
		for (Locomotive loco : locos) result += loco.length;
		for (Car car : cars) result += car.length;
		return result;
	}

	public String name() {
		String result = (name != null ? name : locos.firstElement().name());
		if (direction == null) return result;
		switch (direction) {
		case NORTH:
		case WEST:
			return '←'+result;
		case SOUTH:
		case EAST:
			return result+'→';
		}
		return result;
	}

	public Tag props() {
		Window window = new Window("train-properties",t("Properties of {}",getClass().getSimpleName()));
		
		Tag list = new Tag("ul");
		Tag locos = new Tag("li").content(t("Locomotives:"));
		Tag l2 = new Tag("ul");
		for (Locomotive loco : this.locos) new Tag("li").content(loco.name()).addTo(l2);
		l2.addTo(locos).addTo(list);
		
		new Tag("li").content(t("Current location: {}",block)).addTo(list);
		new Tag("li").content(t("Direction: heading {}",direction)).addTo(list);
		
		new Tag("li").clazz("link").attr("onclick","train("+block.x+","+block.y+",'start')").content(t("start")).addTo(list).addTo(window);
		
		return window;
	}
	
	public void setSpeed(int v) {
		LOG.debug("Setting speed to {} kmh.",v);
		this.speed = v;
	}
	
	public String start() throws IOException {
		if (block == null) return t("{} not in a block",this); 
		HashSet<Route> routes = block.routes();
		Vector<Route> availableRoutes = new Vector<Route>();
		for (Route route : routes) {
			if (route.path().firstElement() != block) continue; // route does not start with current location of loco
			if (direction != null && route.startDirection != direction) continue;
			if (!route.free()) {
				LOG.debug("{} is not free!",route);
				continue;
			}
			availableRoutes.add(route);
		}
		if (route != null) route.unlock().setSignals(Signal.STOP);
		Random rand = new Random();
		if (availableRoutes.isEmpty()) return t("No free routes from {}",block);
		int sel = rand.nextInt(availableRoutes.size());
		this.route = availableRoutes.get(sel).lock(this).setSignals(null);
		setSpeed(100);
		return t("started {}",this); 
	}
	
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	@Override
	public String toString() {
		return name();
	}

	public Train heading(Direction dir) {
		direction = dir;
		return this;
	}
}
