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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.BrakeProcess;
import de.srsoftware.web4rail.threads.DelayedExecution;
import de.srsoftware.web4rail.threads.RoutePrepper;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

/**
 * @author Stephan Richter, SRSoftware 2020-2021 * 
 */
public class Train extends BaseClass implements Comparable<Train> {
	private static final Logger LOG = LoggerFactory.getLogger(Train.class);

	private static final String CAR_ID  = "carId";
	public  static final String LOCO_ID = "locoId";
	private static final String TRACE   = "trace";

	private static final String NAME = "name";

	public static int defaultEndSpeed = 10;
	public static int defaultSpeedStep = 10;
	private String name = null;
	
	
	private static final String ROUTE = "route";
	private Route route;	
		
	private Direction direction;
	private boolean autopilot;
	private static final String PUSH_PULL = "pushPull";
	public boolean pushPull = false;
	
	private static final String CARS = "cars";
	private static final String LOCOS = "locomotives";
	private Vector<Car> cars = new Vector<Car>();
	
	private static final String TAGS = "tags";

	public static final String DESTINATION = "destination";

	private static final String ACTION_REVERSE = "reverse";
	public static final String DESTINATION_PREFIX = "@";
	public static final char TURN_FLAG = '±';
	public static final char FLAG_SEPARATOR = '+';
	public static final char SHUNTING_FLAG = '¥';

	private HashSet<String> tags = new HashSet<String>();
	private boolean f1,f2,f3,f4;

	private Block currentBlock,destination = null;
	HashSet<Tile> trace = new HashSet<Tile>();
	private Vector<Block> lastBlocks = new Vector<Block>();
	
	public int speed = 0;
	private static final String SHUNTING = "shunting";
	private boolean shunting = false;
	private RoutePrepper routePrepper = null;

	private HashSet<Tile> stuckTrace = null;

	private Route nextPreparedRoute;

	private BrakeProcess brake;

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
		Train train = BaseClass.get(id);
		if (isNull(train)) return(t("No train with id {}!",id));
		switch (action) {		
			case ACTION_ADD:
				return train.addCar(params);
			case ACTION_AUTO:
				return train.start(true);
			case ACTION_CONNECT:
				return train.connect(params);
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
				return train.faster(Train.defaultSpeedStep);
			case ACTION_MOVE:
				return train.setDestination(params);
			case ACTION_PROPS:
				return train.properties();
			case ACTION_QUIT:
				return train.properties(train.quitAutopilot());
			case ACTION_REVERSE:
				return train.reverse().properties();
			case ACTION_SLOWER10:
				return train.slower(Train.defaultSpeedStep);
			case ACTION_START:
				return train.properties(train.start(false));
			case ACTION_STOP:
				return train.stopNow();
			case ACTION_TIMES:
				return train.removeBrakeTimes();
			case ACTION_TURN:
				return train.turn().properties();
			case ACTION_UPDATE:
				return train.update(params);		 
		}
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	public Train add(Car car) {
		if (isSet(car)) {
			cars.add(car);
			car.train(this);
		}
		return this;		
	}

	public void addTag(String tag) {
		tags.add(tag);
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

	private Fieldset blockHistory() {
		Fieldset fieldset = new Fieldset(t("Last blocks")).id("props-history");
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
		LOG.debug("generated new {} brake id for {}: {}",reversed?"backward":"forward",this,brakeId);
		return brakeId;
	}
	
	private Fieldset brakeTimes() {
		Fieldset fieldset = new Fieldset(t("Brake time table")).id("props-times");
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
		Table carList = new Table();
		carList.addHead(t("Car"),t("Actions"));

		boolean first = true;
		for (Car car : cars) {
			Tag link = car.link(car.name()+(car.stockId.isEmpty() ? "" : " ("+car.stockId+")"));
			Tag buttons = new Tag("span");
			
			car.button(t("turn within train"),Map.of(ACTION,ACTION_TURN)).addTo(buttons);
			if (!first) {
				car.button("↑",Map.of(ACTION,ACTION_MOVE)).addTo(buttons);
				car.button(t("decouple"),Map.of(ACTION,ACTION_DECOUPLE,REALM,REALM_CAR)).addTo(buttons);
			}			
			button(t("delete"),Map.of(ACTION,ACTION_DROP,CAR_ID,car.id().toString())).addTo(buttons);			
			carList.addRow(link,buttons);
			first = false;
		}
		carList.addTo(locoProp);
		List<Locomotive> locos = BaseClass.listElements(Locomotive.class).stream().filter(loco -> isNull(loco.train())).collect(Collectors.toList());
		if (!locos.isEmpty()) {
			Form addLocoForm = new Form("append-loco-form");
			new Input(REALM, REALM_TRAIN).hideIn(addLocoForm);
			new Input(ACTION, ACTION_ADD).hideIn(addLocoForm);
			new Input(ID,id).hideIn(addLocoForm);
			Select select = new Select(CAR_ID);
			for (Car loco : locos) select.addOption(loco.id(), loco+(loco.stockId.isEmpty()?"":" ("+loco.stockId+")"));
			select.addTo(addLocoForm);
			new Button(t("add"),addLocoForm).addTo(addLocoForm);
			carList.addRow(t("add locomotive"),addLocoForm);
		}
		
		List<Car> cars = BaseClass.listElements(Car.class).stream().filter(car -> !(car instanceof Locomotive)).filter(loco -> isNull(loco.train())).collect(Collectors.toList());
		if (!cars.isEmpty()) {
			Form addCarForm = new Form("append-car-form");
			new Input(REALM, REALM_TRAIN).hideIn(addCarForm);
			new Input(ACTION, ACTION_ADD).hideIn(addCarForm);
			new Input(ID,id).hideIn(addCarForm);
			Select select = new Select(CAR_ID);
			 
			for (Car car : cars) {
				String caption = null;
				
				if (!car.stockId.isEmpty()) caption = car.stockId;
				if (!car.tags().isEmpty()) caption = (isSet(caption) ? caption+" / " :"") + String.join(" ",car.tags());
				caption = car.toString() + (isNull(caption) ? "" : " ("+caption+")");
				select.addOption(car.id(), caption);
			}
			select.addTo(addCarForm);
			new Button(t("add"),addCarForm).addTo(addCarForm);
			carList.addRow(t("add car"),addCarForm);
		}
		if (isSet(currentBlock)) {
			Tag ul = new Tag("ul");
			Train trainInBlock = isSet(currentBlock) ? currentBlock.occupyingTrain() : null;
			if (isSet(trainInBlock) && trainInBlock != this) trainInBlock.link().addTo(new Tag("li")).addTo(ul);
			for (Train tr : currentBlock.trains()) {
				if (tr == this) continue;
				Tag li = new Tag("li").addTo(ul);
				tr.link().addTo(li);
				button(t("couple"),Map.of(ACTION,ACTION_CONNECT,REALM_TRAIN,tr.id().toString())).addTo(li);
			}
			carList.addRow(t("other trains in {}",currentBlock),ul);
			
		}
		
		return locoProp;
	
	}
	
	public List<Car> cars(){
		return new Vector<Car>(cars);
	}
	
	@Override
	public int compareTo(Train o) {
		return name().compareTo(o.toString());
	}
	
	public Window connect(HashMap<String, String> params) {
		Train other = BaseClass.get(new Id(params.get(REALM_TRAIN)));
		if (isSet(other)) coupleWith(other, false);
		return properties();
	}
	
	public Context contact(Contact contact) {
		if (isNull(route)) return new Context(contact).train(this);
		return updateTrace(route.contact(contact));
	}

	
	public void coupleWith(Train parkingTrain,boolean swap) {
		if (isSet(direction) && isSet(parkingTrain.direction) && parkingTrain.direction != direction) parkingTrain.turn();
		if (swap) {
			Vector<Car> dummy = new Vector<Car>();
			for (Car car : parkingTrain.cars) dummy.add(car.train(this));
			dummy.addAll(cars);
			cars = dummy;
		} else {
			for (Car car : parkingTrain.cars) {
				cars.add(car.train(this));
			}
		}
		
		parkingTrain.remove();
		if (isSet(currentBlock)) currentBlock.setTrain(this);
	}
	
	private static Object create(HashMap<String, String> params, Plan plan) {
		String locoId = params.get(Train.LOCO_ID);
		if (isNull(locoId)) return t("Need loco id to create new train!");
		Locomotive loco = BaseClass.get(new Id(locoId));
		if (isNull(loco)) return t("unknown locomotive: {}",params.get(ID));
		Train train = new Train().add(loco);
		train.parent(plan);
		if (params.containsKey(NAME)) train.name(params.get(NAME));
		train.register();
		return train.properties();
	}
	
	public Block currentBlock() {
		return currentBlock;
	}
	

	public Object decoupleAfter(Car car) {
		for (int i=0; i<cars.size();i++) {
			if (car == cars.get(i) && splitAfter(i)) break;
		}
		return properties();
	}
	
	public Block destination(){
		LOG.debug("{}.destination()",this);
		if (isNull(destination)) {
			String destTag = destinationTag();
			LOG.debug("→ processing \"{}\"...",destTag);
			if (isSet(destTag)) {
				destTag = destTag.split(DESTINATION_PREFIX)[1];
				LOG.debug("....processing \"{}\"…",destTag);
				for (int i=destTag.length()-1; i>0; i--) {
					switch (destTag.charAt(i)) {
						case FLAG_SEPARATOR:
							destTag = destTag.substring(0,i);
							i=0;
							break;
						case SHUNTING_FLAG:
							LOG.debug("....enabled shunting option");
							shunting = true; 
							break;
					}
				}
				destination = BaseClass.get(new Id(destTag));				
			}
		}// else LOG.debug("→ heading towards {}",destination);
		return destination;
	}
	
	public String destinationTag() {
		for (String tag : tags()) { // check, if endBlock is in train's destinations
			if (tag.startsWith(DESTINATION_PREFIX)) return tag;
		}
		return null;
	}

	public String directedName() {
		String result = name();
		String mark = autopilot ? "ⓐ" : "";
		if (isNull(direction)) return result;
		switch (direction) {
		case NORTH:
		case WEST:
			return '←'+mark+result;
		case SOUTH:
		case EAST:
			return result+mark+'→';
		}
		return mark+result;
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
		if (cars.isEmpty()) {
			remove();
			return t("Removed train \"{}\"",this);
		}
		return properties();
	}
	
	public boolean drop(Route oldRoute) {
		if (isNull(route)) return true;
		if (route != oldRoute) return false;
		route = null;
		return true;
	}

	
	public void dropTrace() {
		trace.forEach(tile -> tile.free(this));
		trace.clear();
	}
	
	private BrakeProcess endBrake() {
		if (isNull(brake)) return null;
		try {
			return brake.end();
		} finally {
			brake = null;
		}
	}
	
	public void endRoute(Route endedRoute) {
		LOG.debug("{}.endRoute({})",this,endedRoute);
		BrakeProcess brake = endBrake();
		direction = endedRoute.endDirection; // muss vor der Auswertung des Destination-Tags stehen!				
		Block endBlock = endedRoute.endBlock();
		Block startBlock = endedRoute.startBlock();
		shunting = false;
		if (endBlock == destination) {
			destination = null;
			
			String destTag = destinationTag();
			if (isSet(destTag)) {
				LOG.debug("destination list: {}",destTag);
				String[] parts = destTag.split(Train.DESTINATION_PREFIX);
				for (int i=0; i<parts.length;i++) LOG.debug("  part {}: {}",i+1,parts[i]);
				String destId = parts[1];
				LOG.debug("destination tag: {}",destId);
				boolean turn = false;
				
				for (int i=destId.length()-1; i>0; i--) {
					switch (destId.charAt(i)) {
						case Train.FLAG_SEPARATOR:
							destId = destId.substring(0,i);
							i=0;
							break;
						case Train.TURN_FLAG:
							turn = true; 
							LOG.debug("Turn flag is set!");
							break;
					}
				}
				if (destId.equals(endBlock.id().toString())) { 
					if (turn) turn();
					
					// update destination tag: remove and add altered tag:
					removeTag(destTag);
					destTag = destTag.substring(parts[1].length()+1);
					if (destTag.isEmpty()) { // no further destinations
						destTag = null;
					} else addTag(destTag);
				}					
			}
			
			if (isNull(destTag)) {
				quitAutopilot();
				plan.stream(t("{} reached it`s destination!",this));
			}
			
			
		}
		if (isSet(brake)) brake.updateTime();
		Integer waitTime = route.waitTime();
		nextPreparedRoute = route.dropNextPreparedRoute();
		if (isSet(nextPreparedRoute)) LOG.debug("nextPreparedRoute is now {}",nextPreparedRoute);
		if ((!autopilot)|| isNull(nextPreparedRoute) || (isSet(waitTime) && waitTime > 0)) setSpeed(0);
		route = null;
		endBlock.setTrain(this);
		currentBlock = endBlock;
		trace.add(endBlock);
		if (!trace.contains(startBlock)) startBlock.dropTrain(this);
		stuckTrace = null;
		if (autopilot) {
			if (isNull(waitTime)) waitTime = 0;
			if (waitTime>0)	plan.stream(t("{} waiting {} secs",this,(int)(waitTime/1000)));
			new DelayedExecution(waitTime,this) {
			
				@Override
				public void execute() {
					if (autopilot) Train.this.start(false);					
				}
			};
		}
	}

	private Tag faster(int steps) {
		setSpeed(speed+steps);
		return properties();
	}
	
	public boolean getFunction(int num) {
		switch (num) {
		case 1:
			return f1;
		case 2:
			return f2;
		case 3:
			return f3;
		case 4:
			return f4;
		default:
			return false;
		}
	}
	
	private boolean hasLoco() {
		for (Car c:cars) {
			if (c instanceof Locomotive) return true;
		}
		return false;
	}
	
	public boolean hasNextPreparedRoute() {
		return isSet(nextPreparedRoute) || (isSet(route) && isSet(route.getNextPreparedRoute()));
	}

		
	public Train heading(Direction dir) {
		LOG.debug("{}.heading({})",this,dir);
		direction = dir;
		if (isSet(currentBlock)) plan.place(currentBlock);
		return this;
	}	
	
	public boolean isShunting() {
		return shunting;
	}

	public boolean isStoppable() {
		if (speed > 0) return true;
		if (isSet(routePrepper)) return true;
		if (isSet(route)) return true;
		return false;
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
			
			new Train().load(json).parent(plan);			
			
			line = file.readLine();
		}
		file.close();
	}

	public Train load(JSONObject json) {
		pushPull = json.getBoolean(PUSH_PULL);
		if (json.has(DIRECTION)) direction = Direction.valueOf(json.getString(DIRECTION));
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(TAGS))  json.getJSONArray(TAGS ).forEach(elem -> {  tags.add(elem.toString()); });
		if (json.has(LOCOS)) { // for downward compatibility
			for (Object id : json.getJSONArray(LOCOS)) add(BaseClass.get(new Id(""+id)));	
		}		
		for (Object id : json.getJSONArray(CARS)) add(BaseClass.get(new Id(""+id)));
		new LoadCallback() {			
			@Override
			public void afterLoad() {
				if (json.has(TRACE)) json.getJSONArray(TRACE).forEach(elem -> {
					Tile tile = plan.get(new Id(elem.toString()), false);
					tile.setTrain(Train.this);					
					trace.add(tile);
				});
				if (json.has(BLOCK)) {// do not move this up! during set, other fields will be referenced!
					currentBlock = (Block) plan.get(Id.from(json, BLOCK), false);
					if (isSet(currentBlock)) {
						currentBlock.setTrain(Train.this);
						trace.add(currentBlock);
//						currentBlock.add(Train.this, direction);
					}
				}
			}
		};
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
				null, // TODO: show destination here!
				null // TODO: show state of autopilot here
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
		if (isSet(name)) return name;
		if (cars.isEmpty()) return t("emtpy train");
		for (Car car : cars) {
			String name = car.name();
			if (isSet(name)) return name;
		}
		return t("empty train");
	}
	
	private Train name(String newName) {
		this.name = newName;
		return this;
	}
	
	public boolean onTrace(Tile t) {
		return trace.contains(t);
	}

			
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Tag propList = new Tag("ul").clazz("proplist");		
		
		if (isSet(currentBlock)) currentBlock.button(currentBlock.toString()).addTo(new Tag("li").content(t("Current location")+COL)).addTo(propList);
		Tag directionLi = null;
		if (isSet(direction)) directionLi = new Tag("li").content(t("Direction: heading {}",direction)+NBSP);
		if (isNull(directionLi)) directionLi = new Tag("li");
		button(t("reverse"), Map.of(ACTION,ACTION_REVERSE)).title(t("Turns the train, as if it went through a loop.")).addTo(directionLi).addTo(propList);

		Tag dest = new Tag("li").content(t("Destination")+COL);
		if (isNull(destination)) {
			button(t("Select from plan"),Map.of(ACTION,ACTION_MOVE,ASSIGN,DESTINATION)).addTo(dest);
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
		carList().addTo(propList);

		
		formInputs.add(t("Name"), new Input(NAME,name()));
		formInputs.add(t("Shunting"),new Checkbox(SHUNTING, t("train is shunting"), shunting));
		formInputs.add(t("Push-pull train"),new Checkbox(PUSH_PULL, t("Push-pull train"), pushPull));
		formInputs.add(t("Tags"), new Input(TAGS,String.join(", ", tags)));
		
		if (this.hasLoco())	preForm.add(Locomotive.cockpit(this));
		postForm.add(propList.addTo(new Fieldset(t("other train properties")).id("props-other")));
		postForm.add(brakeTimes());
		postForm.add(blockHistory());
		
		
		return super.properties(preForm, formInputs, postForm,errors);
	}

	public String quitAutopilot() {
		if (isSet(routePrepper)) routePrepper.stop();
		if (autopilot) {
			autopilot = false;
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
		return null;	
	}
	
	@Override
	public BaseClass remove() {
		if (isSet(currentBlock)) currentBlock.removeChild(this);
		if (isSet(route)) route.removeChild(this);
		for (Tile t:trace) t.removeChild(this);
		return super.remove();
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
		//if (child == nextRoute) nextRoute = null; // TODO
		if (child == currentBlock) currentBlock = null;
		if (child == destination) destination = null;
		if (child == routePrepper) routePrepper.stop();
		cars.remove(child);
		trace.remove(child);
		super.removeChild(child);
	}
	
	public Iterator<String> removeTag(String tag) {
		tags.remove(tag);
		return tags().iterator();
	}

	/**
	 * This turns the train as if it went through a loop. Example:
	 * before: CabCar→ MiddleCar→ Loco→
	 * after: ←Loco ←MiddleCar ←CabCar 
	 */
	public Train reverse() {
		LOG.debug("train.reverse();");

		if (isSet(direction)) direction = direction.inverse();		
		if (isSet(currentBlock)) {
			if (isNull(direction)) direction = currentBlock.directionA();
			plan.place(currentBlock);
		}
		return this;
	}
	
	public Route route() {
		return route;
	}
	
	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Train train:BaseClass.listElements(Train.class)) file.write(train.json()+"\n");
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

	public Train set(Block newBlock) {
		LOG.debug("{}.set({})",this,newBlock);
		if (isSet(currentBlock)) {
			if (newBlock == currentBlock) return this;
			currentBlock.free(this);
		}
		currentBlock = newBlock;
		if (isSet(currentBlock)) {
			currentBlock.setTrain(this);
			lastBlocks.add(newBlock);
			if (lastBlocks.size()>32) lastBlocks.remove(0);
		}
		return this;
	}
	
	private Object setDestination(HashMap<String, String> params) {
		String dest = params.get(DESTINATION);
		if (isNull(dest)) return properties(t("No destination supplied!"));
		if (dest.isEmpty()) {
			destination = null;
			return properties();
		}
		Tile tile = plan.get(new Id(dest), true);
		if (isNull(tile)) return properties(t("Tile {} not known!",dest));
		if (tile instanceof Block) {
			destination = (Block) tile;
			start(true);
			return t("{} now heading for {}",this,destination);
		}
		return properties(t("{} is not a block!",tile));
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
	
	public Train setRoute(Route newRoute) {
		route = newRoute;
		return this;
	}
	
	public void setSpeed(int newSpeed) {
		LOG.debug("{}.setSpeed({})",this,newSpeed);
		speed = Math.min(newSpeed,maxSpeed());
		if (speed < 0) speed = 0;
		cars.stream().filter(c -> c instanceof Locomotive).forEach(car -> ((Locomotive)car).setSpeed(speed));
		plan.stream(t("Set {} to {} {}",this,speed,speedUnit));
	}

	private Tag slower(int steps) {
		setSpeed(speed-steps);
		return properties();
	}
	
	public boolean splitAfter(int position) {
		if (isNull(currentBlock)) return false; // can only split within blocks!
		Train remaining = new Train();
		int len = cars.size();
		for (int i=0; i<len; i++) {
			if (i>=position) {
				Car car = cars.remove(position);
				LOG.debug("Moving {} from {} to {}",car,this,remaining);
				remaining.add(car);
				if (isNull(remaining.name)) {
					remaining.name = car.name();					
				} else if (remaining.name.length()+car.name().length()<30){
					remaining.name += ", "+car.name();
				}
			} else LOG.debug("Skipping {}",cars.get(i));
		}
		if (remaining.cars.isEmpty()) return false;
		remaining.direction = this.direction;
		this.name = null;
		currentBlock.addParkedTrain(remaining);
		remaining.currentBlock = currentBlock;
		plan.place(currentBlock);
		return true;
	}


	public String start(boolean auto) {
		LOG.debug("{}.start({})",this,auto?"auto":"");
		autopilot |= auto;
		if (isSet(nextPreparedRoute)) {
			LOG.debug("starting nextPreparedRoute: {}",nextPreparedRoute);
			if (nextPreparedRoute.startNow()) {
				LOG.debug("dropped nextPreparedRoute (was {})",nextPreparedRoute);
				nextPreparedRoute = null;
				return null;
			} else {
				LOG.debug("was not able to start {}", nextPreparedRoute);
			}
		}
		if (isSet(routePrepper)) return t("Already searching route for {}",this);
		routePrepper = new RoutePrepper(new Context(this).block(currentBlock).direction(direction));
		
		routePrepper.onRoutePrepared(() -> {
			Route newRoute = routePrepper.route();
			LOG.debug("prepared route {} for {}",newRoute,this);
			newRoute.start();
			routePrepper = null;
			plan.stream(t("Started {}",Train.this));
		});
		
		routePrepper.onFail(() -> {
			LOG.debug("preparing route for {} failed, resetting.",this);
			Route failedRoute = routePrepper.route();
			routePrepper = null;
			if (isSet(failedRoute)) failedRoute.reset();
			LOG.debug("Starting {} failed due to unavailable route!",this);
			plan.onChange(()->{ // wait for state change of plan
				if (autopilot) Train.this.start(false);				
			});
		});
		
		routePrepper.start();		
		
		return null;
	}

	public static void startAll() {
		LOG.debug("Train.startAll()");
		for (Train train : BaseClass.listElements(Train.class)) LOG.info(train.start(true));
	}
	
	public void startBrake() {
		LOG.debug("{}.startBrake()",this);
		if (autopilot && isSet(nextPreparedRoute)) {
			LOG.debug("not braking, because autopilot is active and next roue is prepared, already");
			return;
		}
		brake = new BrakeProcess(this);
	}

	public Window stopNow() {
		endBrake();
		setSpeed(0);
		quitAutopilot();
		if (isSet(route)) {
			stuckTrace = new HashSet<Tile>(); 
			for (Tile tile : route.path()) { // collect occupied tiles of route. stuckTrace is considered during next route search
				if (trace.contains(tile)) stuckTrace.add(tile);
			}
			route.reset();
			route = null;
		}
		return properties();
	}
	
	public HashSet<Tile> stuckTrace() {
		return stuckTrace;
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
	public Train turn() {
		LOG.debug("{}.turn()",this);
		setSpeed(0);
		for (Car car : cars) car.turn();
		reverse(cars);
		return reverse();
	}

	protected Window update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		pushPull = params.containsKey(PUSH_PULL) && params.get(PUSH_PULL).equals("on");
		shunting = params.containsKey(SHUNTING) && params.get(SHUNTING).equals("on");
		if (params.containsKey(NAME)) {
			name = params.get(NAME);
			if (isSet(currentBlock)) plan.place(currentBlock);
		}
		if (params.containsKey(TAGS)) {
			String[] parts = params.get(TAGS).replace(",", " ").split(" ");
			tags.clear();
			for (String tag : parts) {
				tag = tag.trim();
				if (!tag.isEmpty()) tags.add(tag);
			}
		}
		return properties();
	}
	
	public Context updateTrace(Context context) {
		LOG.debug("updateTrace({})",context);
		Tile from = context.tile();
		if (isNull(from)) from = context.contact();
		if (isNull(from)) {
			LOG.debug("no starting point for trace given in {}",context);
			return context;			
		}
		trace.add(from);
		Route route = context.route();
		LOG.debug("Route: {}",route);
		if (isNull(route)) return context;
		Vector<Tile> reversedPath = reverse(route.path());
		HashSet<Tile> newTrace = new HashSet<Tile>();
		Integer remainingLength = null;
		
		for (Tile tile : reversedPath) {
			if (isNull(remainingLength) && onTrace(tile)) remainingLength = length();
			if (remainingLength == null) { // ahead of train
				LOG.debug("{} is ahead of train and will not be touched.",tile);
				trace.remove(tile); // old trace will be cleared afterwards. but this tile shall not be cleared, so remove it from old trace				
			} else if (remainingLength > 0) { // within train
				LOG.debug("{} is occupied by train and will be marked as \"occupied\"",tile);
				remainingLength -= tile.length();
				newTrace.add(tile);
				trace.remove(tile); // old trace will be cleared afterwards. but this tile shall not be cleared, so remove it from old trace
				tile.setTrain(this);
				LOG.debug("remaining length: {}",remainingLength);
			} else { // behind train
				if (Route.freeBehindTrain) {
					LOG.debug("{} is behind train and will be freed in the next step",tile);
					trace.add(tile); // old trace will be cleared afterwards
				} else {
					LOG.debug("{} is behind train and will be reset to \"locked\" state",tile);
					tile.lockFor(context,true);
					trace.remove(tile); // old trace will be cleared afterwards. but this tile shall not be cleared, so remove it from old trace
				}
			}			
		}
		for (Tile tile : trace) tile.free(this);
		trace = newTrace;
		return context;
	}

	public boolean usesAutopilot() {
		return autopilot;
	}
}
