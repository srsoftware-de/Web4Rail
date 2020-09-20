package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.HashMap;
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
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Signal;

public class Train {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);
	private static final String PUSH_PULL = "pushPull";
	private static int count = 0;
	private static final HashMap<Integer, Train> trains = new HashMap<Integer, Train>();
	private static final String ID = "id";
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	private Vector<Car> cars = new Vector<Car>();
	private String name = null;
	private Block block = null;
	public Route route;	
	public int speed = 0;
	private Direction direction;
	private boolean pushPull = false;
	private int id;
	
	public Train(Locomotive loco) {
		id = ++count;
		add(loco);
		trains.put(id, this);
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
	
	public Train heading(Direction dir) {
		direction = dir;
		return this;
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
		
		Form form = new Form();
		new Tag("input").attr("type", "hidden").attr("name","action").attr("value", "updateTrain").addTo(form);
		new Tag("input").attr("type", "hidden").attr("name",ID).attr("value", id).addTo(form);
		
		Checkbox pp = new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull);
		pp.addTo(form);
		new Tag("button").attr("type", "submit").content(t("save")).addTo(form).addTo(window);
		
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
		for (Locomotive loco : locos) loco.setSpeed(v);
		this.speed = v;
	}
	
	public String start() throws IOException {
		if (block == null) return t("{} not in a block",this); 
		if (route != null) route.unlock().setSignals(Signal.STOP);
		HashSet<Route> routes = block.routes();
		Vector<Route> availableRoutes = new Vector<Route>();
		for (Route rt : routes) {
			if (rt == route) continue; // andere Route als zuvor wählen
			if (rt.path().firstElement() != block) continue; // keine Route wählen, die nicht vom aktuellen Block des Zuges startet
			if (direction != null && rt.startDirection != direction) { // Route ist entgegen der Startrichtung des Zuges
				if (!pushPull || !block.turnAllowed) { // Zug ist kein Wendezug oder Block erlaubt kein Wenden
					continue;
				}
			}
			if (!rt.free()) { // keine belegten Routen wählen
				LOG.debug("{} is not free!",rt);
				continue;
			}
			availableRoutes.add(rt);
		}
		Random rand = new Random();
		if (availableRoutes.isEmpty()) return t("No free routes from {}",block);
		int sel = rand.nextInt(availableRoutes.size());
		route = availableRoutes.get(sel).lock(this).setSignals(null);
		if (direction != route.startDirection) turn();
		setSpeed(100);
		return t("started {}",this); 
	}
	
	private static String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	private void turn() throws IOException {
		direction = direction.inverse();
		if (block != null) block.train(this); 
	}


	public static void update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		int id = Integer.parseInt(params.get(ID));
		Train train = trains.get(id);
		if (train == null) return;
		train.pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
	}
}
