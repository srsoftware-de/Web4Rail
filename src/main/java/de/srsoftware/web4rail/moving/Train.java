package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.json.JSONObject;
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
import de.srsoftware.web4rail.tiles.Tile;

public class Train {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);
	
	private static final HashMap<Long, Train> trains = new HashMap<>();
	
	public static final String ID = "id";
	public long id;

	private static final String NAME = "name";
	private String name = null;
	
	
	private static final String ROUTE = "route";
	public Route route;	
		
	private static final String DIRECTION = "direction";
	private Direction direction;
	
	private static final String PUSH_PULL = "pushPull";
	private boolean pushPull = false;
	
	private static final String CARS = "cars";
	private Vector<Car> cars = new Vector<Car>();

	private static final String LOCOS = "locomotives";	
	private Vector<Locomotive> locos = new Vector<Locomotive>();

	private Block block = null;
		
	private class Autopilot extends Thread{
		boolean stop = false;
		
		@Override
		public void run() {
			try {
				stop = false;
				Vector<Tile> path = new Vector<Tile>();
				while (true) {
					if (route == null) {
						Thread.sleep(2000);
						if (stop) return;
						Train.this.start();
						path = route == null ? new Vector<Tile>() : route.path();
					} else {
						if (!path.isEmpty()) {
							Tile t = path.remove(0);
							if (t instanceof Contact) ((Contact)t).activate();
						}
					}
					Thread.sleep(250);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}

	public static final String MODE_START = "start";
	public static final String MODE_SHOW = "show";
	private static final String MODE_UPDATE = "update";
	private static final String MODE_AUTO = "auto";

	private static final String MODE_STOP = "stop";

	public int speed = 0;
	private Autopilot autopilot = null;
	
	public Train(Locomotive loco) {
		this(loco,null);
	}
	
	public Train(Locomotive loco, Long id) {
		if (id == null) id = new Date().getTime();
		this.id = id;
		add(loco);
		trains.put(id, this);
	}

	public static Object action(HashMap<String, String> params) throws IOException {
		if (!params.containsKey(Train.ID)) return t("No train id passed!");
		long id = Long.parseLong(params.get(Train.ID));
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
		case MODE_STOP:
			return train.stop();
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
	
	public static Train get(long id) {
		return trains.get(id);
	}

	
	public Train heading(Direction dir) {
		direction = dir;
		return this;
	}	
	
	private JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(ID, id);
		json.put(NAME,name);
		if (route != null) json.put(ROUTE, route.id());
		if (direction != null) json.put(DIRECTION, direction);
		json.put(PUSH_PULL, pushPull);
		Vector<String> locoIds = new Vector<String>();
		for (Locomotive loco : locos) locoIds.add(loco.id());
		json.put(LOCOS, locoIds);
		Vector<String> carIds = new Vector<String>();
		for (Car car : cars) carIds.add(car.id());
		json.put(CARS,carIds);
		return json;
	}
		
	public int length() {
		int result = 0;		
		for (Locomotive loco : locos) result += loco.length;
		for (Car car : cars) result += car.length;
		return result;
	}
	
	public Tag link(String tagClass) {
		return new Tag(tagClass).clazz("link").attr("onclick","train("+id+",'"+Train.MODE_SHOW+"')").content(name());
	}
	
	public static void loadAll(String filename) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			
			long id = json.getLong(ID);
			
			Train train = new Train(null,id);
			train.load(json);			
			
			line = file.readLine();
		}
		file.close();
	}

	private void load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		for (Object id : json.getJSONArray(LOCOS)) add((Locomotive) Car.get((String)id));
		for (Object id : json.getJSONArray(CARS)) add(Car.get((String)id));
	}
	
	public static Object manager() {
		Window win = new Window("train-manager", t("Train manager"));
		new Tag("h4").content(t("known trains")).addTo(win);
		Tag list = new Tag("ul");
		for (Train train : trains.values()) {
			train.link("li").addTo(list);
		}
		list.addTo(win);
		return win;
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
		new Tag("input").attr("type", "hidden").attr("name",ID).attr("value", ""+id).addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","mode").attr("value", MODE_UPDATE).addTo(form);
		
		Checkbox pp = new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull);
		pp.addTo(form);
		new Tag("button").attr("type", "submit").content(t("save")).addTo(form).addTo(window);
		
		Tag list = new Tag("ul");
		Tag locos = new Tag("li").content(t("Locomotives:"));
		Tag l2 = new Tag("ul");
		for (Locomotive loco : this.locos) new Tag("li").content(loco.name()).addTo(l2);
		l2.addTo(locos).addTo(list);
		
		if (block != null) {
			new Tag("li").content(t("Current location: {}",block)).addTo(list);
			Tag actions = new Tag("li").clazz().content(t("Actions: "));
			new Tag("span").clazz("link").attr("onclick","train("+id+",'"+MODE_START+"')").content(" "+t("start")+" ").addTo(actions);
			if (autopilot == null) {
				new Tag("span").attr("onclick","train("+id+",'"+MODE_AUTO+"')").content(" "+t("auto")+" ").addTo(actions);
			} else {
				new Tag("span").clazz("link").attr("onclick","train("+id+",'"+MODE_STOP+"')").content(" "+t("stop")+" ").addTo(actions);
			}
			actions.addTo(list);

		}
		if (direction != null) new Tag("li").content(t("Direction: heading {}",direction)).addTo(list);
		
		
		list.addTo(window);
		return window;
	}
	
	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<Long, Train> entry:trains.entrySet()) {
			Train train = entry.getValue();
			file.write(train.json()+"\n");
		}
		file.close();
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
	
	private Object stop() {
		autopilot.stop = true;
		autopilot = null;
		return t("{} stopping at next block {}");
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
