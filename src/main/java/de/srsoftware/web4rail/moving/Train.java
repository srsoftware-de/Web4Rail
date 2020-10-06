package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;

public class Train implements Constants {
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

	public static final String LOCO_ID = "locoId";

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
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to Train.action!");
		if (!params.containsKey(Train.ID)) {
			switch (action) {
				case ACTION_PROPS:
					return manager();
				case ACTION_ADD:
					return create(params);
			}
			return t("No train id passed!");
		}
		long id = Long.parseLong(params.get(Train.ID));
		Train train = trains.get(id);
		if (train == null) return(t("No train with id {}!",id));
		switch (action) {
			case ACTION_PROPS:
				return train.props();
			case ACTION_AUTO:
				return train.automatic();
			case ACTION_START:
				return train.start();
			case ACTION_STOP:
				return train.stop();
			case ACTION_UPDATE:
				return train.update(params);		 
		}
		return t("Unknown action: {}",params.get(ACTION));
	}

	private static Object create(HashMap<String, String> params) {
		Locomotive loco = (Locomotive) Locomotive.get(params.get(Train.LOCO_ID));
		if (loco == null) return t("unknown locomotive: {}",params.get(ID));
		Train train = new Train(loco);
		if (params.containsKey(NAME)) train.name(params.get(NAME));
		return train;
	}

	private Train name(String newName) {
		this.name = newName;
		return this;
	}

	public void add(Car car) {
		if (car == null) return;
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);
		car.train(this);
	}
	
	private String automatic() {
		if (autopilot == null) {
			autopilot = new Autopilot();
			autopilot.start();
		}
		return t("{} now in auto-mode",this);
	}
	
	public void block(Block block) throws IOException {
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
		if (name != null)json.put(NAME, name);
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
		return new Tag(tagClass).clazz("link").attr("onclick","train("+id+",'"+ACTION_PROPS+"')").content(name());
	}
	
	public static Collection<Train> list() {
		return trains.values();
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
		if (json.has(NAME)) name = json.getString(NAME);
		for (Object id : json.getJSONArray(CARS)) add(Car.get((String)id));
		for (Object id : json.getJSONArray(LOCOS)) add((Locomotive) Car.get((String)id));
	}
	
	public static Object manager() {
		Window win = new Window("train-manager", t("Train manager"));
		new Tag("h4").content(t("known trains")).addTo(win);
		Tag list = new Tag("ul");
		for (Train train : trains.values()) {
			train.link("li").addTo(list);
		}
		list.addTo(win);
		
		Form form = new Form();
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_TRAIN).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new train"));
		new Input(Train.NAME, t("new train")).addTo(new Label(t("Name:")+" ")).addTo(fieldset);

		Select select = new Select(LOCO_ID);
		for (Locomotive loco : Locomotive.list()) select.addOption(loco.id(),loco.name());
		select.addTo(new Label(t("Locomotive:")+" ")).addTo(fieldset);

		new Button(t("save")).addTo(fieldset);
		fieldset.addTo(form).addTo(win);


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
		Fieldset fieldset = new Fieldset(t("Train properties"));
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_TRAIN).hideIn(form);
		new Input(ID,id).hideIn(form);
		new Input(NAME,name()).addTo(fieldset);
		new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull).addTo(fieldset);
		new Button(t("save")).addTo(fieldset).addTo(form).addTo(window);
		
		Tag list = new Tag("ul");
		if (!locos.isEmpty()) {
			Tag locos = new Tag("li").content(t("Locomotives:"));
			Tag l2 = new Tag("ul");
			for (Locomotive loco : this.locos) loco.link("li").addTo(l2);
			l2.addTo(locos).addTo(list);
		}
		
		if (block != null) {
			new Tag("li").content(t("Current location: {}",block)).addTo(list);
			Tag actions = new Tag("li").clazz().content(t("Actions: "));
			new Button(t("start"),"train("+id+",'"+ACTION_START+"')").addTo(actions);
			if (autopilot == null) {
				new Button(t("auto"),"train("+id+",'"+ACTION_AUTO+"')").addTo(actions);
			} else {
				new Button(t("stop"),"train("+id+",'"+ACTION_STOP+"')").addTo(actions);
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
	
	public CompletableFuture<String> start() throws IOException {
		if (block == null) return CompletableFuture.failedFuture(new RuntimeException(t("{} not in a block",this)));
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
		if (availableRoutes.isEmpty()) return CompletableFuture.failedFuture(new RuntimeException(t("No free routes from {}",block)));
		route = availableRoutes.get(rand.nextInt(availableRoutes.size()));
		return route.lock(this).thenApply(reply -> {
			try {
				route.setSignals(null);
				if (direction != route.startDirection) turn();
				setSpeed(100);
				return t("started {}",this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private Object stop() {
		autopilot.stop = true;
		autopilot = null;
		return t("{} stopping at next block.",this);
	}
	
	private static String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	private void turn() throws IOException {
		LOG.debug("train.turn()");
		if (direction != null) {
			direction = direction.inverse();
			for (Locomotive loco : locos) loco.turn(); 
		}
		if (block != null) block.train(this); 
	}


	public Train update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
		if (params.containsKey(NAME)) name = params.get(NAME);
		return this;
	}
}
