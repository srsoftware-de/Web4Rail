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
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.PathFinder;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

public class Train extends BaseClass implements Comparable<Train> {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);

	private static final String CAR_ID  = "carId";
	public  static final String LOCO_ID = "locoId";
	private static final String TRACE   = "trace";
	private static final HashMap<Id, Train> trains = new HashMap<>();
	public static final String ID = "id";

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
						if (isNull(route)) { // may have been set by start action in between
							Train.this.start();
							if (isSet(destination)) Thread.sleep(1000); // limit load on PathFinder
						}						
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

	private Route nextRoute;
	
	public Train(Locomotive loco) {
		this(loco,null);
	}
	
	public Train(Locomotive loco, Id id) {
		if (isNull(id)) id = new Id();
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
		Id id = Id.from(params);
		Train train = trains.get(id);
		if (isNull(train)) return(t("No train with id {}!",id));
		switch (action) {
			case ACTION_ADD:
				return train.addCar(params);
			case ACTION_AUTO:
				return train.automatic();
			case ACTION_DROP:
				return train.dropCar(params);
			case ACTION_FASTER10:
				return train.faster(10);
			case ACTION_MOVE:
				return train.setDestination(params);
			case ACTION_PROPS:
				return train.properties();
			case ACTION_QUIT:
				return train.quitAutopilot();
			case ACTION_SLOWER10:
				return train.slower(10);
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
		add(car);
		return properties();
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
	
	public String brakeId() {
		return brakeId(false);
	}
	
	public String brakeId(boolean reversed) {
		TreeSet<String> carIds = new TreeSet<String>();
		locos.stream().map(loco -> loco.id()+":"+(reversed != loco.reverse?"r":"f")).forEach(carIds::add);
		cars.stream().map(car -> ""+car.id()).forEach(carIds::add);
		String brakeId = md5sum(carIds);
		LOG.debug("generated new brake id for {}: {}",this,brakeId);
		return brakeId;
	}
	
	private Tag carList() {
		Tag locoProp = new Tag("li").content(t("Cars:"));
		Tag locoList = new Tag("ul").clazz("carlist").content("");

		for (Car car : this.cars) {
			Tag li = new Tag("li");
			car.link(car.name()+(car.stockId.isEmpty() ? "" : " ("+car.stockId+")")).addTo(li).content(NBSP);
			Map<String, Object> params = Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_DROP,CAR_ID,car.id());
			new Button(t("delete"),params).addTo(li);
			li.addTo(locoList);
		}

		Form addCarForm = new Form("append-car-form");
		addCarForm.content(t("add car:")+"&nbsp;");
		new Input(REALM, REALM_TRAIN).hideIn(addCarForm);
		new Input(ACTION, ACTION_ADD).hideIn(addCarForm);
		new Input(ID,id).hideIn(addCarForm);
		Select select = new Select(CAR_ID);
		for (Car car : Car.list()) {
			if (isNull(car.train())) select.addOption(car.id(), car+(car.stockId.isEmpty()?"":" ("+car.stockId+")"));
		}
		if (!select.children().isEmpty()) {
			select.addTo(addCarForm);
			new Button(t("add"),addCarForm).addTo(addCarForm);
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
		Car car = Car.get(params.get(CAR_ID));
		if (isSet(car)) {
			cars.remove(car);
			car.train(null);
		}
		Locomotive loco = Locomotive.get(params.get(LOCO_ID));
		if (isSet(loco)) {
			locos.remove(loco);			
			loco.train(null);
		}
		return properties();
	}
	
	public void dropTrace() {
		while (!trace.isEmpty()) trace.removeFirst().set(null);
	}
	
	private Tag faster(int steps) {
		setSpeed(speed+steps);
		return properties();
	}
		
	public static Train get(Id id) {
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
	
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(PUSH_PULL, pushPull);

		if (isSet(currentBlock)) json.put(BLOCK, currentBlock.id());
		if (isSet(name))json.put(NAME, name);
		if (isSet(route)) json.put(ROUTE, route.id());
		if (isSet(direction)) json.put(DIRECTION, direction);
		
		json.put(LOCOS, locos.stream().map(l -> l.id().toString()).collect(Collectors.toList()));
		json.put(CARS,cars.stream().map(c -> c.id().toString()).collect(Collectors.toList()));
		json.put(TRACE, trace.stream().map(t -> t.id().toString()).collect(Collectors.toList()));
		
		if (!tags.isEmpty()) json.put(TAGS, tags);
		return json;
	}
		
	public int length() {
		int result = 0;		
		for (Locomotive loco : locos) result += loco.length;
		for (Car car : cars) result += car.length;
		return result;
	}
	
	/**
	 * If arguments are given, the first is taken as content, the second as tag type.
	 * If no content is supplied, name is set as content.
	 * If no type is supplied, "span" is preset.
	 * @param args
	 * @return
	 */
	public Tag link(String...args) {
		String tx = args.length<1 ? name()+NBSP : args[0];
		String type = args.length<2 ? "span" : args[1];
		return link(type, tx);
	}
	
	public static TreeSet<Train> list() {
		return new TreeSet<Train>(trains.values());
	}

	public static void loadAll(String filename, Plan plan) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename, UTF8));
		String line = file.readLine();
		while (isSet(line)) {
			JSONObject json = new JSONObject(line);
			
			Train train = new Train(null,Id.from(json));
			train.plan(plan).load(json);			
			
			line = file.readLine();
		}
		file.close();
	}

	public Train load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		if (json.has(DIRECTION)) direction = Direction.valueOf(json.getString(DIRECTION));
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(TAGS))  json.getJSONArray(TAGS ).forEach(elem -> {  tags.add(elem.toString()); });
		if (json.has(TRACE)) json.getJSONArray(TRACE).forEach(elem -> {  trace.add(plan.get(new Id(elem.toString()), false).set(this)); });
		if (json.has(BLOCK)) currentBlock = (Block) plan.get(new Id(json.getString(BLOCK)), false).set(this); // do not move this up! during set, other fields will be referenced!
		for (Object id : json.getJSONArray(CARS)) add(Car.get(id));
		for (Object id : json.getJSONArray(LOCOS)) add((Locomotive) Car.get(id));
		super.load(json);
		return this;
	}
	
	private Tag locoList() {
		Tag locoProp = new Tag("li").content(t("Locomotives:"));
		Tag locoList = new Tag("ul").clazz("locolist");

		for (Locomotive loco : this.locos) {
			Tag li = new Tag("li");
			loco.link(loco.name()+(loco.stockId.isEmpty() ? "" : " ("+loco.stockId+")")).addTo(li);
			Map<String, Object> props = Map.of(REALM,REALM_LOCO,ID,loco.id(),ACTION,ACTION_TURN);
			new Button(t("turn within train"),props).addTo(li).addTo(locoList);
			Map<String, Object> params = Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_DROP,LOCO_ID,loco.id()+(loco.stockId.isEmpty()?"":" ("+loco.stockId+")"));
			new Button(t("delete"),params).addTo(li);
		}

		Form addLocoForm = new Form("append-loco-form");
		addLocoForm.content(t("add locomotive:")+"&nbsp;");
		new Input(REALM, REALM_TRAIN).hideIn(addLocoForm);
		new Input(ACTION, ACTION_ADD).hideIn(addLocoForm);
		new Input(ID,id).hideIn(addLocoForm);
		Select select = new Select(CAR_ID);
		for (Car loco : Locomotive.list()) {
			if (isNull(loco.train())) select.addOption(loco.id(), loco);
		}
		if (!select.children().isEmpty()) {
			select.addTo(addLocoForm);
			new Button(t("add"),addLocoForm).addTo(addLocoForm);
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

		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Name"),t("Length"),t("Max. Speed"),t("Tags"),t("Route"),t("Current location"),t("Destination"),t("Auto pilot"));
		list().forEach(train -> {
			int ms = train.maxSpeed();
			table.addRow(		
				train.link(),
				train.length()+NBSP+lengthUnit,
				ms == Integer.MAX_VALUE ? "–" : ms+NBSP+speedUnit,
				String.join(", ", train.tags()),
				train.route,
				isSet(train.currentBlock) ? train.currentBlock.link() : null,
				train.destination(),
				t(isSet(train.autopilot)?"On":"Off")
			);
		});
		table.addTo(win);
		
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
	
	
	private int maxSpeed() {
		int maxSpeed = Integer.MAX_VALUE;
		for (Locomotive loco : locos) {
			int max = loco.maxSpeed();
			if (max == 0) continue;
			maxSpeed = Math.min(max, maxSpeed);
		}
		for (Car car : cars) {
			int max = car.maxSpeed();
			if (max == 0) continue;
			maxSpeed = Math.min(max, maxSpeed);
		}
		return maxSpeed;
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
	
	public Window properties() {
		
		Fieldset otherTrainProsps = new Fieldset(t("other train properties"));
		
		Tag propList = new Tag("ul").clazz("proplist");
		
		locoList().addTo(propList);
		carList().addTo(propList);
		
		if (isSet(currentBlock)) currentBlock.link("span",currentBlock.toString()).addTo(new Tag("li").content(t("Current location:")+NBSP)).addTo(propList);
		if (isSet(direction)) new Tag("li").content(t("Direction: heading {}",direction)).addTo(propList);

		Tag dest = new Tag("li").content(t("Destination:")+NBSP);
		if (isNull(destination)) {
			new Button(t("Select from plan"),"return selectDest("+id+");").addTo(dest);			
		} else {
			link("span",destination,Map.of(REALM,REALM_PLAN,ID,destination.id().toString(),ACTION,ACTION_CLICK)).addTo(dest);
			new Button(t("Drop"),Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_MOVE,DESTINATION,"")).addTo(dest);
		}
		
		dest.addTo(propList);		
		if (isSet(route)) link("li", route).addTo(propList);
		int ms = maxSpeed();
		if (ms < Integer.MAX_VALUE) new Tag("li").content(t("Max. Speed")+": "+maxSpeed()+NBSP+speedUnit).addTo(propList);
		
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
		
		propList.addTo(otherTrainProsps);
		
		List<Tag> formInputs = List.of(
				new Input(NAME,name),
				new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull),
				new Input(TAGS,String.join(", ", tags)).addTo(new Label(t("Tags")+NBSP))
			); 
		
		return super.properties(List.of(Locomotive.cockpit(this)), formInputs, List.of(otherTrainProsps));
	}

	public Object quitAutopilot() {
		if (isSet(nextRoute)) {
			nextRoute.reset();
			nextRoute = null;
		}
		if (isSet(autopilot)) {
			autopilot.stop = true;
			autopilot = null;			
			return t("{} stopping at next block.",this);
		} else return t("autopilot not active.");
	}

	public void removeFromTrace(Tile tile) {
		trace.remove(tile);		
	}
	
	public void reserveNext() {
		Context context = new Context(this).route(route).block(route.endBlock());
		Route nextRoute = PathFinder.chooseRoute(context);
		if (isNull(nextRoute)) return;
		
		boolean error = !nextRoute.lockIgnoring(route);
		error = error || !nextRoute.setTurnouts();
		error = error || !nextRoute.fireSetupActions(context);

		if (error) {
			nextRoute.reset(); // may unlock tiles belonging to the current route. 
			route.lock(); // corrects unlocked tiles of nextRoute
		} else {
			this.nextRoute = nextRoute;
			this.route.brakeCancel();
		}
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
		for (Entry<Id, Train> entry:trains.entrySet()) {
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
		Tile tile = plan.get(new Id(dest), true);
		if (isNull(tile)) return t("Tile {} not known!",dest);
		if (tile instanceof Block) {
			destination = (Block) tile;
			automatic();
			return t("{} now heading for {}",this,destination);
		}
		return t("{} is not a block!",tile);
	}
	
	public void setSpeed(int newSpeed) {
		speed = Math.min(newSpeed,maxSpeed());
		if (speed < 0) speed = 0;
		for (Locomotive loco : locos) loco.setSpeed(speed);
		plan.stream(t("Set {} to {} {}",this,speed,speedUnit));
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
	
	private Tag slower(int steps) {
		setSpeed(speed-steps);
		return properties();
	}

	public String start() throws IOException {
		if (isNull(currentBlock)) return t("{} not in a block",this);
		if (maxSpeed() == 0) return t("Train has maximum speed of 0 {}, cannot go!",speedUnit);
		if (isSet(route)) route.reset(); // reset route previously chosen

		Context context = new Context(this).block(this.currentBlock);
		String error = null;
		if (isSet(nextRoute)) {
			route = nextRoute;
			if (!route.lock()) return t("Was not able to lock {}",route);
			nextRoute = null;
		} else {
			route = PathFinder.chooseRoute(context);
			if (isNull(route)) return t("No free routes from {}",currentBlock);
			if (!route.lock()) return t("Was not able to lock {}",route);
			if (!route.setTurnouts()) error = t("Was not able to set all turnouts!");
			if (isNull(error) && !route.fireSetupActions(context)) error = t("Was not able to fire all setup actions of route!");
		}
		if (direction != route.startDirection) turn();
		
		if (isNull(error) && !route.train(this)) error = t("Was not able to assign {} to {}!",this,route);
		if (isSet(error)) {
			route.reset();
			route = null;
			return error;
		}
		startSimulation();
		return t("Started {}",this);
	}
	
	public static void startAll() {
		for (Train train : list()) {
			String response = train.automatic();
			LOG.info(response);
		}
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
						if (isNull(route)) break;
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
		if (isSet(nextRoute)) {
			nextRoute.reset();
			nextRoute = null;
		}
		if (isSet(route)) {
			route.reset();
			route.brakeCancel();
			route = null;
		}
		
		return properties();
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
	
	public Tag turn() {
		LOG.debug("train.turn()");
		if (isSet(direction)) {
			direction = direction.inverse();
			for (Locomotive loco : locos) loco.turn();
			reverseTrace();
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
		return properties();
	}

	protected Train update(HashMap<String, String> params) {
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
