package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
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
import de.srsoftware.web4rail.tiles.Signal;

public class Train implements Constants {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);
	
	private static final HashMap<Integer, Train> trains = new HashMap<>();
	
	public static final String ID = "id";
	public int id;

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
				while (true) {
					if (route == null) {
						Thread.sleep(2000);
						if (stop) return;
						Train.this.start();
					}
					Thread.sleep(250);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}

	public static final String LOCO_ID = "locoId";
	private static final String CAR_ID = "carId";

	public int speed = 0;
	private Autopilot autopilot = null;

	private Plan plan;
	
	public Train(Locomotive loco) {
		this(loco,null);
	}
	
	public Train(Locomotive loco, Integer id) {
		if (id == null) id = Application.createId();
		this.id = id;
		add(loco);
		trains.put(id, this);
	}

	public static Object action(HashMap<String, String> params, Plan plan) throws IOException {
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to Train.action!");
		if (!params.containsKey(Train.ID)) {
			switch (action) {
				case ACTION_PROPS:
					return manager();
				case ACTION_ADD:					
					return create(params,plan);
			}
			return t("No train id passed!");
		}
		int id = Integer.parseInt(params.get(Train.ID));
		Train train = trains.get(id);
		if (train == null) return(t("No train with id {}!",id));
		switch (action) {
			case ACTION_ADD:
				return train.addCar(params);
			case ACTION_AUTO:
				return train.automatic();
			case ACTION_PROPS:
				return train.props();
			case ACTION_QUIT:
				return train.quitAutopilot();
			case ACTION_START:
				return train.start();
			case ACTION_STOP:
				return train.stopNow();
			case ACTION_TURN:
				return train.turn();
			case ACTION_UPDATE:
				return train.update(params);		 
		}
		return t("Unknown action: {}",params.get(ACTION));
	}

	private Object addCar(HashMap<String, String> params) {
		LOG.debug("addCar({})",params);
		if (!params.containsKey(CAR_ID)) return t("No car id passed to Train.addCar!");
		Car car = Car.get(params.get(CAR_ID));
		if (car == null) return t("No car with id \"{}\" known!",params.get(CAR_ID));
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);
		return this;
	}
	
	private static Object create(HashMap<String, String> params, Plan plan) {
		Locomotive loco = (Locomotive) Locomotive.get(params.get(Train.LOCO_ID));
		if (loco == null) return t("unknown locomotive: {}",params.get(ID));
		Train train = new Train(loco).plan(plan);
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
	
	public Block block() {
		return block;
	}
	
	public void block(Block block) throws IOException {
		this.block = block;
	}
	
	public static Train get(int id) {
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

	public static void loadAll(String filename, Plan plan) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			
			int id = json.getInt(ID);
			
			Train train = new Train(null,id);
			train.load(json).plan(plan);			
			
			line = file.readLine();
		}
		file.close();
	}

	private Train load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		if (json.has(NAME)) name = json.getString(NAME);
		for (Object id : json.getJSONArray(CARS)) add(Car.get((String)id));
		for (Object id : json.getJSONArray(LOCOS)) add((Locomotive) Car.get((String)id));
		return this;
	}
	
	private Tag locoList() {
		Tag locoProp = new Tag("li").content(t("Locomotives:"));
		Tag locoList = new Tag("ul").clazz("locolist");

		for (Locomotive loco : this.locos) {
			Tag li = loco.link("li");
			Map<String, Object> props = Map.of(REALM,REALM_LOCO,ID,loco.id(),ACTION,ACTION_TURN);
			new Button(t("turn within train"),props).addTo(li).addTo(locoList);
		}

		Tag addLocoForm = new Form().content(t("add locomotive:")+"&nbsp;");
		new Input(REALM, REALM_TRAIN).hideIn(addLocoForm);
		new Input(ACTION, ACTION_ADD).hideIn(addLocoForm);
		new Input(ID,id).hideIn(addLocoForm);
		Select select = new Select(CAR_ID);
		for (Locomotive loco : Locomotive.list()) {
			if (!this.locos.contains(loco)) select.addOption(loco.id(), loco);
		}
		if (!select.children().isEmpty()) {
			select.addTo(addLocoForm);
			new Button(t("add")).addTo(addLocoForm);
			addLocoForm.addTo(new Tag("li")).addTo(locoList);
		}
		return locoList.addTo(locoProp);
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
	
	private Train plan(Plan plan) {
		this.plan = plan;
		return this;
	}
	
	public Tag props() {
		Window window = new Window("train-properties",t("Properties of {}",this));
		
		Fieldset fieldset = new Fieldset(t("editable train properties"));
		Form form = new Form();
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_TRAIN).hideIn(form);
		new Input(ID,id).hideIn(form);
		new Input(NAME,name).addTo(form);
		new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull).addTo(form);
		new Button(t("save")).addTo(form).addTo(fieldset);
		
		new Button(t("Turn"), "train("+id+",'"+ACTION_TURN+"')").addTo(fieldset).addTo(window);
		
		fieldset = new Fieldset(t("other train properties"));
		
		Tag propList = new Tag("ul").clazz("proplist");
		
		locoList().addTo(propList);
		
		if (block != null) {
			new Tag("li").content(t("Current location: {}",block)).addTo(propList);
			Tag actions = new Tag("li").clazz().content(t("Actions: "));
			new Button(t("start"),"train("+id+",'"+ACTION_START+"')").addTo(actions);
			if (autopilot == null) {
				new Button(t("auto"),"train("+id+",'"+ACTION_AUTO+"')").addTo(actions);
			} else {
				new Button(t("quit autopilot"),"train("+id+",'"+ACTION_QUIT+"')").addTo(actions);
			}
			actions.addTo(propList);

		}
		if (route != null) {
			new Tag("li").content(t("Current route: {}",route)).addTo(propList);
		}
		if (direction != null) new Tag("li").content(t("Direction: heading {}",direction)).addTo(propList);
		
		
		propList.addTo(fieldset).addTo(window);
		return window;
	}

	public Object quitAutopilot() {
		if (autopilot != null) {
			autopilot.stop = true;
			autopilot = null;
			return t("{} stopping at next block.",this);
		} else return t("autopilot not active.");
	}
	
	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<Integer, Train> entry:trains.entrySet()) {
			Train train = entry.getValue();
			file.write(train.json()+"\n");
		}
		file.close();
	}
	
	public static Select selector(Train preselected,Collection<Train> exclude) {
		if (exclude == null) exclude = new Vector<Train>();
		Select select = new Select(Train.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Train train : Train.list()) {			
			if (exclude.contains(train)) continue;
			Tag opt = select.addOption(train.id, train);
			if (train == preselected) opt.attr("selected", "selected");
		}
		return select;
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
		route = availableRoutes.get(rand.nextInt(availableRoutes.size()));
		
		if (!route.lock()) return t("Was not able to lock {}",route);
		String error = null;
		if (!route.setTurnouts()) error = t("Was not able to set all turnouts!");
		if (error == null && !route.setSignals(null)) error = t("Was not able to set all signals!");
		if (error == null && !route.train(this)) error = t("Was not able to assign {} to {}!",this,route);
		if (error == null) {
			setSpeed(128);
			return t("Started {}",this);
		}
		route.unlock();
		this.block.train(this); // re-set train on previous block
		this.route = null;
		return error;				
	}
	
	private Object stopNow() {
		quitAutopilot();
		setSpeed(0);
		if (route != null) try {
			route.unlock();
			route.endBlock().train(null);
			route.startBlock().train(this);
		} catch (IOException e) {
			e.printStackTrace();			
		}
		route = null;
		return t("Stopped {}.",this);
	}
	
	private static String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	@Override
	public String toString() {
		return name != null ? name : locos.firstElement().name();
	}
	
	public Object turn() {
		LOG.debug("train.turn()");
		if (direction != null) {
			direction = direction.inverse();
			for (Locomotive loco : locos) loco.turn(); 
		}
		if (block != null) try {
			plan.place(block.train(this));
		} catch (IOException e) {}
		return t("{} turned.",this);
	}


	public Train update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
		if (params.containsKey(NAME)) name = params.get(NAME);
		return this;
	}
}
