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
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Signal;

public class Train {
	
	private class Autopilot extends Thread{
		@Override
		public void run() {
			try {
				Vector<Contact> contacts = null;
				while (true) {
					if (route == null) {
						Train.this.start();
						contacts = route == null ? new Vector<Contact>() : route.contacts();
					} else {
						if (!contacts.isEmpty()) {
							Contact contact = contacts.remove(0);
							contact.activate();
						}
					}
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);
	private static final String PUSH_PULL = "pushPull";
	private static int count = 0;
	private static final HashMap<Integer, Train> trains = new HashMap<Integer, Train>();
	public static final String ID = "id";
	public static final String MODE_START = "start";
	public static final String MODE_SHOW = "show";
	private static final String MODE_UPDATE = "update";
	private static final String MODE_AUTO = "auto";
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	private Vector<Car> cars = new Vector<Car>();
	private String name = null;
	private Block block = null;
	public Route route;	
	public int speed = 0;
	private Direction direction;
	private boolean pushPull = false;
	public int id;
	private Autopilot autopilot = null;
	
	public Train(Locomotive loco) {
		id = ++count;
		add(loco);
		trains.put(id, this);
	}
	
	public static Object action(HashMap<String, String> params) throws IOException {
		if (!params.containsKey(Train.ID)) return t("No train id passed!");
		int id = Integer.parseInt(params.get(Train.ID));
		Train train = trains.get(id);
		if (train == null) return(t("No train with id {}!",id));
		if (!params.containsKey("mode")) return t("No mode set for train action!");
		String mode = params.get("mode");
		switch (mode) {
		case MODE_AUTO:
			return train.automatic();
		case MODE_SHOW:
			return train.props();
		case MODE_START:
			return train.start();
		case MODE_UPDATE:
			return train.update(params);
		default: return t("Unknown mode {} for {}",mode,train); 
		}
		
		//return null;
	}

	public void add(Car car) {
		if (car == null) return;
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);		
	}
	
	private String automatic() {
		if (autopilot == null) {
			autopilot = new Autopilot();
			autopilot.start();
		}
		return t("{} now in auto-mode",this);
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
		new Tag("input").attr("type", "hidden").attr("name","action").attr("value", "train").addTo(form);
		new Tag("input").attr("type", "hidden").attr("name",ID).attr("value", id).addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","mode").attr("value", MODE_UPDATE).addTo(form);
		
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
		
		new Tag("li").clazz("link").attr("onclick","train("+id+",'"+MODE_START+"')").content(t("start")).addTo(list).addTo(window);
		new Tag("li").clazz("link").attr("onclick","train("+id+",'"+MODE_AUTO+"')").content(t("auto")).addTo(list).addTo(window);
		
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
		if (direction != null) direction = direction.inverse();
		if (block != null) block.train(this); 
	}


	public Train update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
		return this;
	}
}
