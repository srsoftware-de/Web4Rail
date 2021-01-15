package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
import de.srsoftware.web4rail.Application;
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
	private static final String LOCOS = "locomotives";
	private Vector<Car> cars = new Vector<Car>();
	
	private static final String TAGS = "tags";

	private static final String DESTINATION = "destination";

	private static final String ACTION_REVERSE = "reverse";
	
	private HashSet<String> tags = new HashSet<String>();
	private boolean f1,f2,f3,f4;

	private Block currentBlock,destination = null;
	LinkedList<Tile> trace = new LinkedList<Tile>();
	private Vector<Block> lastBlocks = new Vector<Block>();
	
	public int speed = 0;
	private Autopilot autopilot = null;
	private Route nextRoute;

	
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
							Object o = Train.this.start();
							LOG.debug("{}.start called, route now is {}",this,route);
							if (isSet(route)) {
								if (o instanceof String) plan.stream((String)o);
								//if (isSet(destination)) Thread.sleep(1000); // limit load on PathFinder
							} else waitTime = 1000; // limit load on PathFinder
						}						
					} else Thread.sleep(250);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			autopilot = null;
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
	}
	
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
			case ACTION_TOGGLE_F1:
				return train.toggleFunction(1);
			case ACTION_TOGGLE_F2:
				return train.toggleFunction(2);
			case ACTION_TOGGLE_F3:
				return train.toggleFunction(3);
			case ACTION_TOGGLE_F4:
				return train.toggleFunction(4);
			case ACTION_FASTER10:
				return train.faster(10);
			case ACTION_MOVE:
				return train.setDestination(params);
			case ACTION_PROPS:
				return train.properties();
			case ACTION_QUIT:
				return train.quitAutopilot();
			case ACTION_REVERSE:
				return train.reverse();
			case ACTION_SLOWER10:
				return train.slower(10);
			case ACTION_START:
				return train.start();
			case ACTION_STOP:
				return train.stopNow();
			case ACTION_TIMES:
				return train.removeBrakeTimes();
			case ACTION_TURN:
				return train.turn();
			case ACTION_UPDATE:
				return train.update(params);		 
		}
		return t("Unknown action: {}",params.get(ACTION));
	}

	public void addToTrace(Vector<Tile> newTiles) {
		Route.LOG.debug("{}.addToTrace({})",this,newTiles);
		Route.LOG.debug("old trace: {}",trace);
		boolean active = trace.isEmpty();
		for (Tile tile : newTiles) {
			if (active) {
				trace.addFirst(tile);
			} else {
				if (trace.getFirst() == tile) active = true;
			}			
		}
		if (!active) { // newTiles and old trace do not "touch" : add all new tiles
			for (Tile tile : newTiles) trace.addFirst(tile);
		}
		Route.LOG.debug("new trace: {}",trace);
		showTrace();
	}

	private Object addCar(HashMap<String, String> params) {
		LOG.debug("addCar({})",params);
		String carId = params.get(CAR_ID);
		if (isNull(carId)) return t("No car id passed to Train.addCar!");
		Car car = BaseClass.get(new Id(carId));
		if (isNull(car)) return t("No car with id \"{}\" known!",params.get(CAR_ID));
		add(car);
		return properties();
	}

	public void add(Car car) {
		if (isNull(car)) return;
		cars.add(car);
		car.train(this);
	}
	
	public String automatic() {
		if (isNull(autopilot)) {
			autopilot = new Autopilot();
			Application.threadPool.execute(autopilot);
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
		return t("{} now in auto-mode",this);
	}
	
	private Fieldset blockHistory() {
		Fieldset fieldset = new Fieldset(t("Last blocks"));
		Tag list = new Tag("ol");
		for (int i=lastBlocks.size(); i>0; i--) {
			lastBlocks.get(i-1).link().addTo(new Tag("li")).addTo(list);
		}
		return list.addTo(fieldset);
	}

	
	public String brakeId() {
		return brakeId(false);
	}
	
	public String brakeId(boolean reversed) {
		TreeSet<String> carIds = new TreeSet<String>();
		cars.stream().map(car -> car.id()+":"+(car.orientation == reversed ? "r":"f")).forEach(carIds::add);		
		String brakeId = md5sum(carIds);
		LOG.debug("generated new brake id for {}: {}",this,brakeId);
		return brakeId;
	}
	
	private Fieldset brakeTimes() {
		Fieldset fieldset = new Fieldset(t("Brake time table"));
		Table timeTable = new Table();
		timeTable.addRow(t("forward"),t("backward"),t("Route"));
		List<Route> routes = BaseClass.listElements(Route.class);
		Collections.sort(routes, (r1,r2)->r1.name().compareTo(r2.name()));
		String forwardId = brakeId(false);
		String backwardId = brakeId(true);
		for (Route route: routes) {
			Integer forwardTime = route.brakeTime(forwardId);
			Integer reverseTime = route.brakeTime(backwardId);
			timeTable.addRow(isSet(forwardTime)?forwardTime+" ms":"-",isSet(reverseTime)?reverseTime+" ms":"-",route.name());
		}
		
		timeTable.addTo(fieldset);
		this.button(t("Drop brake times"),Map.of(ACTION,ACTION_TIMES)).addTo(fieldset);
		return fieldset;
	}
	
	private Tag carList() {
		Tag locoProp = new Tag("li").content(t("Locomotives and cars")+":");
		Tag carList = new Tag("ul").clazz("carlist");

		for (Car car : this.cars) {
			Tag li = new Tag("li");
			car.link(car.name()+(car.stockId.isEmpty() ? "" : " ("+car.stockId+")")).addTo(li).content(NBSP);
			car.button(t("turn within train"),Map.of(ACTION,ACTION_TURN)).addTo(li);
			car.button("↑",Map.of(ACTION,ACTION_MOVE)).addTo(li);
			button(t("delete"),Map.of(ACTION,ACTION_DROP,CAR_ID,car.id().toString())).addTo(li);			
			li.addTo(carList);
		}

		List<Locomotive> locos = BaseClass.listElements(Locomotive.class).stream().filter(loco -> isNull(loco.train())).collect(Collectors.toList());
		if (!locos.isEmpty()) {
			Form addLocoForm = new Form("append-loco-form");
			addLocoForm.content(t("add locomotive")+COL);
			new Input(REALM, REALM_TRAIN).hideIn(addLocoForm);
			new Input(ACTION, ACTION_ADD).hideIn(addLocoForm);
			new Input(ID,id).hideIn(addLocoForm);
			Select select = new Select(CAR_ID);
			for (Car loco : locos) select.addOption(loco.id(), loco+(loco.stockId.isEmpty()?"":" ("+loco.stockId+")"));
			select.addTo(addLocoForm);
			new Button(t("add"),addLocoForm).addTo(addLocoForm);
			addLocoForm.addTo(new Tag("li")).addTo(carList);
		}
		
		List<Car> cars = BaseClass.listElements(Car.class).stream().filter(car -> !(car instanceof Locomotive)).filter(loco -> isNull(loco.train())).collect(Collectors.toList());
		if (!cars.isEmpty()) {
			Form addCarForm = new Form("append-car-form");
			addCarForm.content(t("add car")+COL);
			new Input(REALM, REALM_TRAIN).hideIn(addCarForm);
			new Input(ACTION, ACTION_ADD).hideIn(addCarForm);
			new Input(ID,id).hideIn(addCarForm);
			Select select = new Select(CAR_ID);
			for (Car car : cars) select.addOption(car.id(), car+(car.stockId.isEmpty()?"":" ("+car.stockId+")"));
				select.addTo(addCarForm);
				new Button(t("add"),addCarForm).addTo(addCarForm);
				addCarForm.addTo(new Tag("li")).addTo(carList);
			}
		return carList.addTo(locoProp);
	
	}
	
	public List<Car> cars(){
		return new Vector<Car>(cars);
	}
	
	@Override
	public int compareTo(Train o) {
		return name().compareTo(o.toString());
	}
	
	private static Object create(HashMap<String, String> params, Plan plan) {
		String locoId = params.get(Train.LOCO_ID);
		if (isNull(locoId)) return t("Need loco id to create new train!");
		Locomotive loco = BaseClass.get(new Id(locoId));
		if (isNull(loco)) return t("unknown locomotive: {}",params.get(ID));
		Train train = new Train(loco);
		train.parent(plan);
		if (params.containsKey(NAME)) train.name(params.get(NAME));
		train.register();
		return train.properties();
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
		String mark = isSet(autopilot) ? "ⓐ" : "";
		if (isNull(direction)) return result;
		switch (direction) {
		case NORTH:
		case WEST:
			return '←'+mark+result;
		case SOUTH:
		case EAST:
			return result+mark+'→';
		}
		return result;
	}

	public Direction direction() {
		return direction;
	}
		
	private Object dropCar(HashMap<String, String> params) {
		String carId = params.get(CAR_ID);
		if (isNull(carId)) return t("Cannot drop car without car id!");
		Car car = BaseClass.get(new Id(carId));		
		if (isSet(car)) {
			cars.remove(car);
			car.train(null);
		}
		return properties();
	}
	
	public void dropTrace() {
		while (!trace.isEmpty()) trace.removeFirst().setTrain(null);
	}
	
	private Tag faster(int steps) {
		setSpeed(speed+steps);
		return properties();
	}
		
	public static Train get(Id id) {
		return trains.get(id);
	}
	
	public Train heading(Direction dir) {
		LOG.debug("{}.heading({})",this,dir);
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
		
		json.put(CARS,cars.stream().map(c -> c.id().toString()).collect(Collectors.toList()));
		json.put(TRACE, trace.stream().map(t -> t.id().toString()).collect(Collectors.toList()));
		
		if (!tags.isEmpty()) json.put(TAGS, tags);
		return json;
	}
	
	public Collection<Block> lastBlocks(int count) {
		Vector<Block> blocks = new Vector<Block>(count);
		for (int i=0; i<count && i<lastBlocks.size(); i++) blocks.add(lastBlocks.get(i));
		return blocks;
	}
		
	public int length() {
		int result = 0;		
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
	
	public static void loadAll(String filename, Plan plan) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename, UTF8));
		String line = file.readLine();
		while (isSet(line)) {
			JSONObject json = new JSONObject(line);
			
			Train train = new Train(null,Id.from(json));
			train.load(json).parent(plan);			
			
			line = file.readLine();
		}
		file.close();
	}

	public Train load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		if (json.has(DIRECTION)) direction = Direction.valueOf(json.getString(DIRECTION));
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(TAGS))  json.getJSONArray(TAGS ).forEach(elem -> {  tags.add(elem.toString()); });
		if (json.has(TRACE)) json.getJSONArray(TRACE).forEach(elem -> {  trace.add(plan.get(new Id(elem.toString()), false).setTrain(this)); });
		if (json.has(BLOCK)) currentBlock = (Block) plan.get(new Id(json.getString(BLOCK)), false).setTrain(this); // do not move this up! during set, other fields will be referenced!
		if (json.has(LOCOS)) { // for downward compatibility
			for (Object id : json.getJSONArray(LOCOS)) add(BaseClass.get(new Id(""+id)));	
		}		
		for (Object id : json.getJSONArray(CARS)) add(BaseClass.get(new Id(""+id)));
		super.load(json);
		return this;
	}
	
	public static Object manager() {
		Window win = new Window("train-manager", t("Train manager"));
		new Tag("h4").content(t("known trains")).addTo(win);

		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Name"),t("Length"),t("Maximum Speed"),t("Tags"),t("Route"),t("Current location"),t("Destination"),t("Auto pilot"));
		BaseClass.listElements(Train.class).forEach(train -> {
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
		
		Form form = new Form("create-train-form");
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_TRAIN).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new train"));
		new Input(Train.NAME, t("new train")).addTo(new Label(t("Name")+COL)).addTo(fieldset);

		Select select = new Select(LOCO_ID);
		BaseClass.listElements(Locomotive.class)
			.stream()
			.filter(loco -> isNull(loco.train()))
			.sorted((l1,l2)->l1.name().compareTo(l2.name()))
			.forEach(loco -> select.addOption(loco.id(),loco.name()));
		select.addTo(new Label(t("Locomotive")+COL)).addTo(fieldset);

		new Button(t("Apply"),form).addTo(fieldset);
		fieldset.addTo(form).addTo(win);

		return win;
	}
	
	
	private int maxSpeed() {
		int maxSpeed = Integer.MAX_VALUE;
		for (Car car : cars) {
			int max = car.maxSpeed();
			if (max == 0) continue;
			maxSpeed = Math.min(max, maxSpeed);
		}
		return maxSpeed;
	}
	
	public Window moveUp(Car car) {
		for (int i = 1; i<cars.size(); i++) {
			if (cars.get(i) == car) {
				cars.remove(i);
				cars.insertElementAt(car, i-1);
				break;
			}
		}
		return properties();
	}


	public String name() {
		return (isSet(name) ? name : cars.stream().filter(car -> isSet(car.name())).findFirst().get().name());
	}
	
	private Train name(String newName) {
		this.name = newName;
		return this;
	}
	
	public Route nextRoute() {
		return nextRoute;
	}


	public boolean nextRoutePrepared() {
		return isSet(nextRoute) && nextRoute.state() == Route.State.PREPARED;
	}
	
	public boolean onTrace(Tile t) {
		return trace.contains(t);
	}

			
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Fieldset otherTrainProps = new Fieldset(t("other train properties"));
		
		Tag propList = new Tag("ul").clazz("proplist");
		
		carList().addTo(propList);
		
		if (isSet(currentBlock)) currentBlock.button(currentBlock.toString()).addTo(new Tag("li").content(t("Current location")+COL)).addTo(propList);
		Tag directionLi = null;
		if (isSet(direction)) directionLi = new Tag("li").content(t("Direction: heading {}",direction)+NBSP);
		if (isNull(directionLi)) directionLi = new Tag("li");
		button(t("reverse"), Map.of(ACTION,ACTION_REVERSE)).title(t("Turns the train, as if it went through a loop.")).addTo(directionLi).addTo(propList);

		Tag dest = new Tag("li").content(t("Destination")+COL);
		if (isNull(destination)) {
			new Button(t("Select from plan"),"return selectDest("+id+");").addTo(dest);			
		} else {
			link("span",destination,Map.of(REALM,REALM_PLAN,ID,destination.id().toString(),ACTION,ACTION_CLICK)).addTo(dest);
			new Button(t("Drop"),Map.of(REALM,REALM_TRAIN,ID,id,ACTION,ACTION_MOVE,DESTINATION,"")).addTo(dest);
		}
		
		dest.addTo(propList);		
		if (isSet(route)) route.link("li", route).addTo(propList);
		int ms = maxSpeed();
		if (ms < Integer.MAX_VALUE) new Tag("li").content(t("Maximum Speed")+COL+maxSpeed()+NBSP+speedUnit).addTo(propList);
		
		SortedSet<String> allTags = tags();
		if (!allTags.isEmpty()) {
			Tag tagList = new Tag("ul");
			for (String tag : allTags) new Tag("li").content(tag).addTo(tagList);
			tagList.addTo(new Tag("li").content(t("Tags"))).addTo(propList);
		}
		new Tag("li").content(t("length: {}",length())+NBSP+lengthUnit).addTo(propList);
		
		if (!trace.isEmpty()) {
			Tag li = new Tag("li").content(t("Occupied area")+COL);
			Tag ul = new Tag("ul");
			for (Tile tile : trace) new Tag("li").content(tile.toString()).addTo(ul);
			ul.addTo(li).addTo(propList);
		}
		
		propList.addTo(otherTrainProps);
		
		formInputs.add(t("Name"), new Input(NAME,name));
		formInputs.add(t("Push-pull train"),new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull));
		formInputs.add(t("Tags"), new Input(TAGS,String.join(", ", tags)));
		
		preForm.add(Locomotive.cockpit(this));
		postForm.add(otherTrainProps);
		postForm.add(brakeTimes());
		postForm.add(blockHistory());
		
		
		return super.properties(preForm, formInputs, postForm);
	}

	public Object quitAutopilot() {
		if (isSet(nextRoute)) {
			nextRoute.reset();
			nextRoute = null;
		}
		if (isSet(autopilot)) {
			autopilot.stop = true;
			autopilot = null;
			if (isSet(currentBlock)) plan.place(currentBlock);
			return t("{} stopping at next block.",this);
		} else return t("autopilot not active.");
	}
	
	private Window removeBrakeTimes() {
		List<Route> routes = BaseClass.listElements(Route.class);
		for (Route route: routes) route.dropBraketimes(brakeId(false),brakeId(true));
		return properties();
	}
	
	@Override
	public void removeChild(BaseClass child) {
		LOG.debug("{}.removeChild({})",this,child);
		if (child == route) route = null;
		if (child == nextRoute) nextRoute = null;
		if (child == currentBlock) currentBlock = null;
		if (child == destination) destination = null;
		cars.remove(child);
		trace.remove(child);
		super.removeChild(child);
	}
	
	public void reserveNext() {
		LOG.debug("{}.reserveNext()",this);
		Context context = new Context(this).route(route).block(route.endBlock()).direction(route.endDirection);
		Route nextRoute = PathFinder.chooseRoute(context);
		if (isNull(nextRoute)) {
			LOG.debug("{}.reserveNext() found no available route!",this);
			return;
		}
		nextRoute.set(context);
		boolean error = !nextRoute.lockIgnoring(route);
		error = error || !nextRoute.fireSetupActions();

		if (error) {
			nextRoute.reset(); // may unlock tiles belonging to the current route. 
			route.lock(); // corrects unlocked tiles of nextRoute
		} else {
			this.nextRoute = nextRoute;
			this.route.brakeCancel();
		}
	}
	
	/**
	 * This turns the train as if it went through a loop. Example:
	 * before: CabCar→ MiddleCar→ Loco→
	 * after: ←Loco ←MiddleCar ←CabCar 
	 */
	public Tag reverse() {
		LOG.debug("train.reverse();");

		if (isSet(direction)) {
			direction = direction.inverse();
			reverseTrace();
		}
		if (isSet(currentBlock)) plan.place(currentBlock);
		return properties();
	}
	
	private void reverseTrace() {
		LinkedList<Tile> reversed = new LinkedList<Tile>();
		LOG.debug("Trace: {}",trace);
		while (!trace.isEmpty()) reversed.addFirst(trace.removeFirst());
		trace = reversed;
		LOG.debug("reversed: {}",trace);
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

		List<Train> trains = BaseClass.listElements(Train.class);
		trains.sort((t1,t2)->t1.name().compareTo(t2.name()));
		for (Train train : trains) {			
			if (exclude.contains(train)) continue;
			Tag opt = select.addOption(train.id, train);
			if (train == preselected) opt.attr("selected", "selected");
		}
		return select;
	}

	public void set(Block newBlock) {
		LOG.debug("{}.set({})",this,newBlock);
		currentBlock = newBlock;
		if (isSet(currentBlock)) {
			currentBlock.setTrain(this);
			lastBlocks.add(newBlock);
			if (lastBlocks.size()>32) lastBlocks.remove(0);
		}
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
	
	public Object setFunction(int num, boolean active) {
		switch (num) {
		case 1:
			f1 = active;	
			break;
		case 2:
			f2 = active;	
			break;
		case 3:
			f3 = active;	
			break;
		case 4:
			f4 = active;
			break;
		default:
			return t("Unknown function: {}",num);
		}
		for (Car car : cars) {
			if (car instanceof Locomotive) {
				Locomotive loco = (Locomotive) car;
				loco.setFunction(num,active);
			}
		}
		return properties();
	}
	
	public void setSpeed(int newSpeed) {
		LOG.debug("{}.setSpeed({})",this,newSpeed);
		speed = Math.min(newSpeed,maxSpeed());
		if (speed < 0) speed = 0;
		cars.stream().filter(c -> c instanceof Locomotive).forEach(car -> ((Locomotive)car).setSpeed(speed));
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
		Route.LOG.debug("{}.showTrace()",this);
 		int remainingLength = length();
 		if (remainingLength<1) remainingLength=1;
		for (int i=0; i<trace.size(); i++) {
			Tile tile = trace.get(i);
			Route.LOG.debug("current tile: {}, remaining length: {}",tile,remainingLength);
			if (remainingLength>0) {
				remainingLength-=tile.length();
				tile.setTrain(this);
			} else {
				tile.setTrain(null);
				if (Route.freeBehindTrain) try {
					tile.unset(route);
				} catch (IllegalArgumentException e) {}
				trace.remove(i);
				i--; // do not move to next index: remove shifted the next index towards us
			}
		}
		Route.LOG.debug("remaining length: {}",remainingLength);
	}
	
	private Tag slower(int steps) {
		setSpeed(speed-steps);
		return properties();
	}

	public Object start() throws IOException {
		LOG.debug("{}.start()",this);
		if (isNull(currentBlock)) return t("{} not in a block",this);
		if (maxSpeed() == 0) return t("Train has maximum speed of 0 {}, cannot go!",speedUnit);
		if (isSet(route)) route.reset(); // reset route previously chosen

		String error = null;
		if (isSet(nextRoute)) {
			LOG.debug("{}.nextRoute = {}",this,nextRoute);
			route = nextRoute;
			if (!route.lock()) return t("Was not able to lock {}",route);
			nextRoute = null;
			route.set(new Context(this).block(currentBlock).direction(direction));			
		} else {
			Context context = new Context(this).block(currentBlock).direction(direction);
			route = PathFinder.chooseRoute(context);
			if (isNull(route)) return t("No free routes from {}",currentBlock);
			if (!route.lock()) error = t("Was not able to lock {}",route);
			route.set(context);
			if (isNull(error) && !route.fireSetupActions()) error = t("Was not able to fire all setup actions of route!");
		}
		if (isNull(error) && direction != route.startDirection) turn();
		
		if (isNull(error) && !route.start(this)) error = t("Was not able to assign {} to {}!",this,route);
		if (isSet(error)) {
			LOG.debug("{}.start:error = {}",this,error);
			route.reset();
			route = null;
			return error;
		}
		startSimulation();
		Window win = properties();
		new Tag("p").content(t("Started {}",this)).addTo(win);
		return win;
	}
	
	public static void startAll() {
		for (Train train : BaseClass.listElements(Train.class)) LOG.info(train.automatic());
	}
	
	private void startSimulation() {
		LOG.debug("{}.startSimulation({})",this);
		for (Contact contact : route.contacts()) {
			if (contact.addr() != 0) {
				LOG.debug("{}.startSimulation aborted!",this);
				return; // simulate train only when all contacts are non-physical
			}
		}
		try {
			Thread.sleep(1000);
			plan.stream(t("Simulating movement of {}...",this));
			Application.threadPool.execute(new Thread() {
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
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Object stopNow() {
		quitAutopilot();
		if (isSet(route)) {
			route.brakeCancel();
			route.reset();
			route = null;
		}
		setSpeed(0);
		
		return properties();
	}

	public SortedSet<String> tags() {
		TreeSet<String> list = new TreeSet<String>(tags);
		for (Car car:cars) list.addAll(car.tags());
		return list;
	}
	
	
	public Object toggleFunction(int f) {
		switch (f) {
		case 1:
			return setFunction(1, !f1);
		case 2:
			return setFunction(2, !f2);
		case 3:
			return setFunction(3, !f3);
		case 4:
			return setFunction(4, !f4);
		}
		return t("Unknown function: {}",f);
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	/**
	 * this inverts the direction the train is heading to. Example:
	 * before: CabCar→ MiddleCar→ Loco→
	 * after: ←CabCar ←MiddleCar ←Loco 
	 * @return 
	 */
	public Tag turn() {
		LOG.debug("{}.turn()",this);
		for (Car car : cars) car.turn();
		Collections.reverse(cars);
		return reverse();
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
