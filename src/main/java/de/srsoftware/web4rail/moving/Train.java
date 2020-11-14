package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.PathFinder;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

public class Train extends BaseClass implements Comparable<Train> {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);

	private static final String CAR_ID  = "carId";
	public  static final String LOCO_ID = "locoId";
	private static final String TRACE   = "trace";
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
	public boolean pushPull = false;
	
	private static final String CARS = "cars";
	private Vector<Car> cars = new Vector<Car>();

	private static final String LOCOS = "locomotives";	
	private Vector<Locomotive> locos = new Vector<Locomotive>();
	
	private static final String TAGS = "tags";

	private static final String DESTINATION = "destination";
	
	private HashSet<String> tags = new HashSet<String>();

	private Block currentBlock,destination = null;
	LinkedList<Tile> trace = new LinkedList<Tile>();
			
	private class Autopilot extends Thread{
		boolean stop = false;
		int waitTime = 100;
		
		@Override
		public void run() {
			try {
				stop = false;
				while (true) {
					if (isNull(route)) {
						Thread.sleep(waitTime);
						if (waitTime > 100) waitTime /=2;
						if (stop) return;
						Train.this.start();
						if (isSet(destination)) Thread.sleep(1000); // limit load on PathFinder
					} else Thread.sleep(250);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
	}

	public int speed = 0;
	private Autopilot autopilot = null;

	private Plan plan;
	
	public Train(Locomotive loco) {
		this(loco,null);
	}
	
	public Train(Locomotive loco, Integer id) {
		if (isNull(id)) id = Application.createId();
		this.id = id;
		add(loco);
		trains.put(id, this);
	}

	public static Object action(HashMap<String, String> params, Plan plan) throws IOException {
		String action = params.get(ACTION);
		if (isNull(action)) return t("No action passed to Train.action!");
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
		if (isNull(train)) return(t("No train with id {}!",id));
		switch (action) {
			case ACTION_ADD:
				return train.addCar(params);
			case ACTION_AUTO:
				return train.automatic();
			case ACTION_DROP:
				return train.dropCar(params);
			case ACTION_MOVE:
				return train.setDestination(params);
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

	public void addToTrace(Vector<Tile> newTiles) {
		boolean active = trace.isEmpty();
		for (Tile tile : newTiles) {
			if (active) {
				trace.addFirst(tile);
			} else {
				Tile dummy = trace.getFirst();
				if (dummy == tile) active = true;
			}			
		}
		showTrace();
	}

	private Object addCar(HashMap<String, String> params) {
		LOG.debug("addCar({})",params);
		if (!params.containsKey(CAR_ID)) return t("No car id passed to Train.addCar!");
		Car car = Car.get(params.get(CAR_ID));
		if (isNull(car)) return t("No car with id \"{}\" known!",params.get(CAR_ID));
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);
		return this;
	}

	public void add(Car car) {
		if (isNull(car)) return;
		if (car instanceof Locomotive) {
			locos.add((Locomotive) car);
		} else cars.add(car);
		car.train(this);
	}
	
	private String automatic() {
		if (isNull(autopilot)) {
			autopilot = new Autopilot();
			autopilot.start();
		}
		return t("{} now in auto-mode",this);
	}
	
	private Tag carList() {
		Tag locoProp = new Tag("li").content(t("Cars:"));
		Tag locoList = new Tag("ul").clazz("carlist").content("");

		for (Car car : this.cars) {
			Tag li = new Tag("li");
			car.link("span").addTo(li).content(NBSP);
			Map<String, Object> params = Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_DROP,CAR_ID,car.id());
			new Button(t("delete"),params).addTo(li);
			li.addTo(locoList);
		}

		Tag addCarForm = new Form().content(t("add car:")+"&nbsp;");
		new Input(REALM, REALM_TRAIN).hideIn(addCarForm);
		new Input(ACTION, ACTION_ADD).hideIn(addCarForm);
		new Input(ID,id).hideIn(addCarForm);
		Select select = new Select(CAR_ID);
		for (Car car : Car.list()) {
			if (!this.cars.contains(car)) select.addOption(car.id(), car+(car.stockId.isEmpty()?"":" ("+car.stockId+")"));
		}
		if (!select.children().isEmpty()) {
			select.addTo(addCarForm);
			new Button(t("add")).addTo(addCarForm);
			addCarForm.addTo(new Tag("li")).addTo(locoList);
		}
		return locoList.addTo(locoProp);
	}
	
	public List<Car> cars(){
		return new Vector<Car>(cars);
	}
	
	@Override
	public int compareTo(Train o) {
		return name().compareTo(o.toString());
	}
	
	private static Object create(HashMap<String, String> params, Plan plan) {
		Locomotive loco = (Locomotive) Locomotive.get(params.get(Train.LOCO_ID));
		if (isNull(loco)) return t("unknown locomotive: {}",params.get(ID));
		Train train = new Train(loco).plan(plan);
		if (params.containsKey(NAME)) train.name(params.get(NAME));
		return train;
	}
	
	public Block currentBlock() {
		return currentBlock;
	}
	
	public Block destination() {
		return destination;
	}
	
	public Train destination(Block dest) {
		destination = dest;
		return this;
	}

	public String directedName() {
		String result = name();
		if (isSet(autopilot)) result="•"+result;
		if (isNull(direction)) return result;
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

	public Direction direction() {
		return direction;
	}
		
	private Object dropCar(HashMap<String, String> params) {
		String carId = params.get(CAR_ID);
		if (isSet(carId)) cars.remove(Car.get(carId));
		String locoId = params.get(LOCO_ID);
		if (isSet(locoId)) locos.remove(Car.get(locoId));
		return props();
	}
	
	public void dropTrace() {
		while (!trace.isEmpty()) trace.removeFirst().set(null);
	}
	
	public static Train get(int id) {
		return trains.get(id);
	}
	
	public Train heading(Direction dir) {
		direction = dir;
		if (isSet(currentBlock)) plan.place(currentBlock);
		return this;
	}	
	
	public Tile headPos() {
		return trace.getFirst();
	}
	
	private JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(ID, id);
		json.put(PUSH_PULL, pushPull);

		if (isSet(currentBlock)) json.put(BLOCK, currentBlock.id());
		if (isSet(name))json.put(NAME, name);
		if (isSet(route)) json.put(ROUTE, route.id());
		if (isSet(direction)) json.put(DIRECTION, direction);

		Vector<Integer> locoIds = new Vector<Integer>();
		for (Locomotive loco : locos) locoIds.add(loco.id());
		json.put(LOCOS, locoIds);
		
		Vector<Integer> carIds = new Vector<Integer>();
		for (Car car : cars) carIds.add(car.id());
		json.put(CARS,carIds);
		
		Vector<String> tileIds = new Vector<String>();
		for (Tile tile : trace) tileIds.add(tile.id());
		json.put(TRACE, tileIds);
		
		if (!tags.isEmpty()) json.put(TAGS, tags);
		return json;
	}
		
	public int length() {
		int result = 0;		
		for (Locomotive loco : locos) result += loco.length;
		for (Car car : cars) result += car.length;
		return result;
	}
	
	public Tag link(String tagClass) {
		return link(tagClass, Map.of(REALM, REALM_TRAIN,ID,id,ACTION,ACTION_PROPS),name()+NBSP);
	}
	
	public static TreeSet<Train> list() {
		return new TreeSet<Train>(trains.values());
	}

	public static void loadAll(String filename, Plan plan) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (isSet(line)) {
			JSONObject json = new JSONObject(line);
			
			int id = json.getInt(ID);
			
			Train train = new Train(null,id);
			train.plan(plan).load(json);			
			
			line = file.readLine();
		}
		file.close();
	}

	private Train load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		if (json.has(DIRECTION)) direction = Direction.valueOf(json.getString(DIRECTION));
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(TAGS))  json.getJSONArray(TAGS ).forEach(elem -> {  tags.add(elem.toString()); });
		if (json.has(TRACE)) json.getJSONArray(TRACE).forEach(elem -> {  trace.add(plan.get(elem.toString(), false).set(this)); });
		if (json.has(BLOCK)) currentBlock = (Block) plan.get(json.getString(BLOCK), false).set(this); // do not move this up! during set, other fields will be referenced!
		for (Object id : json.getJSONArray(CARS)) add(Car.get(id));
		for (Object id : json.getJSONArray(LOCOS)) add((Locomotive) Car.get(id));
		return this;
	}
	
	private Tag locoList() {
		Tag locoProp = new Tag("li").content(t("Locomotives:"));
		Tag locoList = new Tag("ul").clazz("locolist");

		for (Locomotive loco : this.locos) {
			Tag li = new Tag("li");
			loco.link("span").addTo(li);
			Map<String, Object> props = Map.of(REALM,REALM_LOCO,ID,loco.id(),ACTION,ACTION_TURN);
			new Button(t("turn within train"),props).addTo(li).addTo(locoList);
			Map<String, Object> params = Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_DROP,LOCO_ID,loco.id()+(loco.stockId.isEmpty()?"":" ("+loco.stockId+")"));
			new Button(t("delete"),params).addTo(li);
		}

		Tag addLocoForm = new Form().content(t("add locomotive:")+"&nbsp;");
		new Input(REALM, REALM_TRAIN).hideIn(addLocoForm);
		new Input(ACTION, ACTION_ADD).hideIn(addLocoForm);
		new Input(ID,id).hideIn(addLocoForm);
		Select select = new Select(CAR_ID);
		for (Car loco : Locomotive.list()) {
			if (!this.locos.contains(loco)) select.addOption(loco.id(), loco);
		}
		if (!select.children().isEmpty()) {
			select.addTo(addLocoForm);
			new Button(t("add")).addTo(addLocoForm);
			addLocoForm.addTo(new Tag("li")).addTo(locoList);
		}
		return locoList.addTo(locoProp);
	}
	
	public List<Car> locos(){
		return new Vector<Car>(locos);
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
		new Input(Train.NAME, t("new train")).addTo(new Label(t("Name:")+NBSP)).addTo(fieldset);

		Select select = new Select(LOCO_ID);
		for (Car loco : Locomotive.list()) select.addOption(loco.id(),loco.name());
		select.addTo(new Label(t("Locomotive:")+NBSP)).addTo(fieldset);

		new Button(t("Apply")).addTo(fieldset);
		fieldset.addTo(form).addTo(win);


		return win;
	}

	public String name() {
		return (isSet(name) ? name : locos.firstElement().name());
	}
	
	private Train name(String newName) {
		this.name = newName;
		return this;
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
		new Input(TAGS,String.join(", ", tags)).addTo(new Label(t("Tags")+NBSP)).addTo(form);
		new Button(t("Apply")).addTo(form).addTo(fieldset);
		
		HashMap<String, Object> props = new HashMap<String,Object>(Map.of(REALM,REALM_TRAIN,ID,id));
		props.put(ACTION, ACTION_TURN);
		new Button(t("Turn"), props).addTo(fieldset).addTo(window);
		
		fieldset = new Fieldset(t("other train properties"));
		
		Tag propList = new Tag("ul").clazz("proplist");
		
		locoList().addTo(propList);
		carList().addTo(propList);
		
		if (isSet(currentBlock)) {
			link("li",Map.of(REALM,REALM_PLAN,ID,currentBlock.id(),ACTION,ACTION_CLICK),t("Current location: {}",currentBlock)).addTo(propList);
			Tag actions = new Tag("li").clazz().content(t("Actions:")+NBSP);
			if (isSet(route)) {
				props.put(ACTION, ACTION_STOP);
				new Button(t("stop"),props).addTo(actions);
			} else {
				props.put(ACTION, ACTION_START);
				new Button(t("start"),props).addTo(actions);
			}
			if (isNull(autopilot)) {
				props.put(ACTION, ACTION_AUTO);
				new Button(t("auto"),props).addTo(actions);
			} else {
				props.put(ACTION, ACTION_QUIT);
				new Button(t("quit autopilot"),props).addTo(actions);
			}
			actions.addTo(propList);
		}
		
		Tag dest = new Tag("li").content(t("Destination:")+NBSP);
		if (isNull(destination)) {
			new Button(t("Select from plan"),"return selectDest("+id+");").addTo(dest);			
		} else {
			link("span",Map.of(REALM,REALM_PLAN,ID,destination.id(),ACTION,ACTION_CLICK),destination.toString()).addTo(dest);
			new Button(t("Drop"),Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_MOVE,DESTINATION,"")).addTo(dest);
		}
		
		dest.addTo(propList);		
		if (isSet(route)) {
			link("li", Map.of(REALM,REALM_ROUTE,ID,route.id(),ACTION,ACTION_PROPS), route).addTo(propList);
		}
		if (isSet(direction)) new Tag("li").content(t("Direction: heading {}",direction)).addTo(propList);
		
		SortedSet<String> allTags = tags();
		if (!allTags.isEmpty()) {
			Tag tagList = new Tag("ul");
			for (String tag : allTags) new Tag("li").content(tag).addTo(tagList);
			tagList.addTo(new Tag("li").content(t("Tags"))).addTo(propList);
		}
		new Tag("li").content(t("length: {}",length())).addTo(propList);
		
		if (!trace.isEmpty()) {
			Tag li = new Tag("li").content(t("Occupied area:"));
			Tag ul = new Tag("ul");
			for (Tile tile : trace) new Tag("li").content(tile.toString()).addTo(ul);
			ul.addTo(li).addTo(propList);
		}
		
		propList.addTo(fieldset).addTo(window);
		return window;
	}

	public Object quitAutopilot() {
		if (isSet(autopilot)) {
			autopilot.stop = true;
			autopilot = null;
			return t("{} stopping at next block.",this);
		} else return t("autopilot not active.");
	}

	public void removeFromTrace(Tile tile) {
		trace.remove(tile);		
	}
	
	private void reverseTrace() {
		LinkedList<Tile> reversed = new LinkedList<Tile>();
		LOG.debug("Trace: {}",trace);
		while (!trace.isEmpty()) reversed.addFirst(trace.removeFirst());
		trace = reversed;
		LOG.debug("reversed: {}",trace);
		reversed = null;
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
		if (isNull(exclude)) exclude = new Vector<Train>();
		Select select = new Select(Train.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Train train : Train.list()) {			
			if (exclude.contains(train)) continue;
			Tag opt = select.addOption(train.id, train);
			if (train == preselected) opt.attr("selected", "selected");
		}
		return select;
	}

	public void set(Block newBlock) {
		currentBlock = newBlock;
		if (isSet(currentBlock)) currentBlock.set(this);
	}
	
	private String setDestination(HashMap<String, String> params) {
		String dest = params.get(DESTINATION);
		if (isNull(dest)) return t("No destination supplied!");
		if (dest.isEmpty()) {
			destination = null;
			return t("Dropped destination of {}.",this);
		}
		Tile tile = plan.get(dest, true);
		if (isNull(tile)) return t("Tile {} not known!",dest);
		if (tile instanceof Block) {
			destination = (Block) tile;
			return t("{} now heading for {}",this,destination);
		}
		return t("{} is not a block!",tile);
	}
	
	public void setSpeed(int v) {
		for (Locomotive loco : locos) loco.setSpeed(v);
		plan.stream(t("Set {} to {} km/h",this,v));
		this.speed = v;
	}
	
	public void setWaitTime(Range waitTime) {
		if (isNull(autopilot)) return; 
		autopilot.waitTime = waitTime.random();
		String msg = t("{} waiting {} secs...",this,autopilot.waitTime/1000d);
		LOG.debug(msg);
		plan.stream(msg);

	}
	
	public void showTrace() {
 		int remainingLength = length();
 		if (remainingLength<1) remainingLength=1;
		for (int i=0; i<trace.size(); i++) {
			Tile tile = trace.get(i);
			if (remainingLength>0) {
				remainingLength-=tile.length();
				tile.set(this);
			} else {
				tile.set(null);
				trace.remove(i);
				i--; // do not move to next index: remove shifted the next index towards us
			}
		}
	}
	
	public String start() throws IOException {
		if (isNull(currentBlock)) return t("{} not in a block",this);
		if (isSet(route)) route.reset(); // reset route previously chosen
		
		Context context = new Context(this);
		route = PathFinder.chooseRoute(context);
		if (isNull(route)) return t("No free routes from {}",currentBlock);		
		if (!route.lock()) return t("Was not able to lock {}",route);
		
		if (direction != route.startDirection) turn();
		
		String error = null;
		if (!route.setTurnouts()) error = t("Was not able to set all turnouts!");
		if (isNull(error) && !route.fireSetupActions(context)) error = t("Was not able to fire all setup actions of route!");
		if (isNull(error) && !route.train(this)) error = t("Was not able to assign {} to {}!",this,route);
		if (isSet(error)) {
			route.reset();
			route = null;
			return error;
		}
		startSimulation();
		return t("Started {}",this);
	}
	
	private void startSimulation() {
		for (Contact contact : route.contacts()) {
			if (contact.addr() != 0) return; // simulate train only when all contacts are non-physical
		}
		try {
			Thread.sleep(1000);
			plan.stream(t("Simulating movement of {}...",this));
			new Thread() {
				public void run() {
					for (Tile tile : route.path()) {
						try {
							if (tile instanceof Contact) {
								Contact contact = (Contact) tile;
								contact.activate(true);
								sleep(200);
								contact.activate(false);
							}
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				};
			}.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Object stopNow() {
		quitAutopilot();
		setSpeed(0);
		if (isSet(route)) {
			route.reset();
			route = null;
		}
		return t("Stopped {}.",this);
	}
	
	private static String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	

	public SortedSet<String> tags() {
		TreeSet<String> list = new TreeSet<String>(tags);
		for (Locomotive loco : locos) list.addAll(loco.tags());
		for (Car car:cars) list.addAll(car.tags());
		return list;
	}
	
	@Override
	public String toString() {
		return isSet(name) ? name : locos.firstElement().name();
	}
	
	public Object turn() {
		LOG.debug("train.turn()");
		if (isSet(direction)) {
			direction = direction.inverse();
			for (Locomotive loco : locos) loco.turn();
			reverseTrace();
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
		return t("{} turned.",this);
	}

	public Train update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
		if (params.containsKey(NAME)) name = params.get(NAME);
		if (params.containsKey(TAGS)) {
			String[] parts = params.get(TAGS).replace(",", " ").split(" ");
			tags.clear();
			for (String tag : parts) {
				tag = tag.trim();
				if (!tag.isEmpty()) tags.add(tag);
			}
		}

		return this;
	}
	
	public boolean usesAutopilot() {
		return isSet(autopilot);
	}
}
