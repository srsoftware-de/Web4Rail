package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tiles.Block;

public class Locomotive extends Car implements Constants,Device{
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	boolean reverse = false;
	private Protocol proto = Protocol.DCC128;
	private int address = 3;
	private int speed = 0;
	private boolean f1,f2,f3,f4;
	private boolean init = false;

	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, Id id) {
		super(name,id);
	}
	
	public static Object action(HashMap<String, String> params, Plan plan) throws IOException {
		String id = params.get(ID);
		Locomotive loco = id == null ? null : Locomotive.get(id);
		switch (params.get(ACTION)) {
			case ACTION_ADD:
				new Locomotive(params.get(Locomotive.NAME)).parent(plan);
				return Locomotive.manager();
			case ACTION_FASTER10:
				return loco.faster(10);
			case ACTION_PROPS:
				return loco == null ? Locomotive.manager() : loco.properties();
			case ACTION_SLOWER10:
				return loco.faster(-10);
			case ACTION_STOP:
				return loco.stop();
			case ACTION_TOGGLE_F1:
				return loco.toggleFunction(1);
			case ACTION_TOGGLE_F2:
				return loco.toggleFunction(2);
			case ACTION_TOGGLE_F3:
				return loco.toggleFunction(3);
			case ACTION_TOGGLE_F4:
				return loco.toggleFunction(4);
			case ACTION_TURN:
				return loco.turn();
			case ACTION_UPDATE:
				loco.update(params);
				return Locomotive.manager();
		}
		
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	@Override
	public int address() {
		return address;
	}
	
	public static Fieldset cockpit(Object locoOrTrain) {
		Id id = null;
		int speed = 0;
		String realm = null;
		Train train = null;
		Locomotive loco = null;
		if (locoOrTrain instanceof Locomotive) {
			loco = (Locomotive) locoOrTrain; 
			realm = REALM_LOCO;
			id = loco.id();
			speed = loco.speed;
		} else if (locoOrTrain instanceof Train) {
			train = (Train)locoOrTrain;
			realm = REALM_TRAIN;			
			id = train.id();
			speed = train.speed;
		}
		
		HashMap<String,Object> params = new HashMap<String, Object>(Map.of(REALM,realm,ID,id));
		
		Fieldset fieldset = new Fieldset(t("Control"));
		fieldset.clazz("cockpit");
		
		new Tag("span").content(t("Current velocity: {} {}",speed,speedUnit)).addTo(fieldset);
		
		Tag par = new Tag("p");
		Map.of(t("Slower (10 {})",speedUnit),ACTION_SLOWER10,t("Faster (10 {})",speedUnit),ACTION_FASTER10).entrySet().forEach(e -> {
			params.put(ACTION, e.getValue());
			new Button(t(e.getKey()),params).addTo(par);			
		});
		par.addTo(fieldset);

		Tag direction = new Tag("p");
		if ((isSet(train) && train.speed > 0) || (isSet(loco) && loco.speed > 0)) {
			params.put(ACTION, ACTION_STOP);
			new Button(t("Stop"),params).clazz(ACTION_STOP).addTo(direction);			
		}

		params.put(ACTION, ACTION_TURN);
		new Button(t("Turn"),params).clazz(ACTION_TURN).addTo(direction);			

		if (isSet(train)) {
			Block currentBlock = train.currentBlock();
			if (isSet(currentBlock)) {
				if (isNull(train.route)) {
					params.put(ACTION, ACTION_START);
					new Button(t("start"),params).addTo(direction);
				}
				if (train.usesAutopilot()) {
					params.put(ACTION, ACTION_QUIT);
					new Button(t("quit autopilot"),params).addTo(direction);
				} else {
					params.put(ACTION, ACTION_AUTO);
					new Button(t("auto"),params).addTo(direction);
				}
			}
		}
		direction.addTo(fieldset);
		
		Tag functions = new Tag("p");
		Map.of("F1",ACTION_TOGGLE_F1,"F2",ACTION_TOGGLE_F2,"F3",ACTION_TOGGLE_F3,"F4",ACTION_TOGGLE_F4).entrySet().forEach(e -> {
			params.put(ACTION, e.getValue());
			new Button(t(e.getKey()),params).addTo(functions);			
		});
		functions.addTo(fieldset);
		

		return fieldset;
	}
	
	private String detail() {
		return getClass().getSimpleName()+"("+name()+", "+proto+", "+address+")";
	}

	
	public Object faster(int steps) {
		setSpeed(speed + steps);
		return properties();
	}
	
	public static Locomotive get(Object id) {		
		Car car = Car.get(id);
		if (car instanceof Locomotive) return (Locomotive) car;
		return null;
	}
	
	private void init() {
		if (init) return;
		String proto = null;
		switch (this.proto) {
		case FLEISCH:
			proto = "F"; break;
		case MOTO:
			proto = "M 2 100 0"; break; // TODO: make configurable
		case DCC14:
		case DCC27:
		case DCC28:
		case DCC128:
			proto = "N 1 "+this.proto.steps+" 5"; break; // TODO: make configurable
		case SELECTRIX:
			proto = "S"; break;
		}
		plan.queue(new Command("INIT {} GL "+address+" "+proto) {
			
			@Override
			public void onSuccess() {
				super.onSuccess();
				plan.stream(t("{} initialized.",this));
			}
			
			@Override
			public void onFailure(Reply r) {
				super.onFailure(r);
				plan.stream(t("Was not able to initialize {}!",this));				
			}
		});
		init = true;
	}


	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONObject loco = new JSONObject();
		loco.put(REVERSE, reverse);
		loco.put(PROTOCOL, proto);
		loco.put(ADDRESS, address);		
		json.put(LOCOMOTIVE, loco);
		return json;
	}
	
	@Override
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(REVERSE)) reverse = loco.getBoolean(REVERSE);
			if (loco.has(PROTOCOL)) proto = Protocol.valueOf(loco.getString(PROTOCOL));
			if (loco.has(ADDRESS)) address = loco.getInt(ADDRESS);
		}
		return this;
	}	

	public static Window manager() {
		Window win = new Window("loco-manager", t("Locomotive manager"));
		new Tag("h4").content(t("known locomotives")).addTo(win);
		
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Protocol"),t("Address"),t("Length"),t("Tags"));
		cars.values()
			.stream()
			.filter(car -> car instanceof Locomotive)
			.map(car -> (Locomotive)car)
			.sorted(Comparator.comparing(loco -> loco.address))
			.sorted(Comparator.comparing(loco -> loco.stockId))
			.forEach(loco -> table.addRow(loco.stockId,loco.link(),loco.maxSpeed == 0 ? "–":loco.maxSpeed+NBSP+speedUnit,loco.proto,loco.address,loco.length+NBSP+lengthUnit,String.join(", ", loco.tags())));
		table.addTo(win);

		
		Form form = new Form("add-loco-form");
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_LOCO).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new locomotive"));
		new Input(Locomotive.NAME, t("new locomotive")).addTo(new Label(t("Name:")+NBSP)).addTo(fieldset);
		new Button(t("Apply"),form).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		preForm.add(cockpit(this));
		Tag div = new Tag("div");
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.proto).addTo(div);
		}
		formInputs.add(t("Protocol"),div);
		formInputs.add(t("Address"),new Input(ADDRESS, address).numeric());
		return super.properties(preForm, formInputs, postForm);
	}
	
	private void queue() {
		int step = proto.steps * speed / (maxSpeed == 0 ? 100 : maxSpeed); 
		plan.queue(new Command("SET {} GL "+address+" "+(reverse?1:0)+" "+step+" "+proto.steps+" "+(f1?1:0)+" "+(f2?1:0)+" "+(f3?1:0)+" "+(f4?1:0)) {

			@Override
			public void onFailure(Reply reply) {
				super.onFailure(reply);
				plan.stream(t("Failed to send command to {}: {}",this,reply.message()));
			}			
		});
	}

	/**
	 * Sets the speed of the locomotive to the given velocity in [plan.speedUnit]s
	 * @param newSpeed
	 * @return
	 */
	public String setSpeed(int newSpeed) {
		LOG.debug(this.detail()+".setSpeed({})",newSpeed);
		init();
		speed = newSpeed;
		if (speed > maxSpeed && maxSpeed > 0) speed = maxSpeed();
		if (speed < 0) speed = 0;
		
		queue();
		return t("Speed of {} set to {}.",this,speed);
	}
	
	public Object stop() {
		setSpeed(0);
		return properties();
	}
	
	private Object toggleFunction(int f) {
		boolean active; 
		switch (f) {
		case 1:
			f1 =! f1;	
			active = f1;
			break;
		case 2:
			f2 =! f2;	
			active = f2;
			break;
		case 3:
			f3 =! f3;	
			active = f3;
			break;
		case 4:
			f4 =! f4;	
			active = f4;
			break;
		default:
			return t("Unknown function: {}",f);
		}
		queue();
		return t("{} F{}",t(active?"Activated":"Deavtivated"),f);
	}
	
	public Object turn() {
		reverse = !reverse;
		stop();
		return t("Stopped and reversed {}.",this);
	}
	
	@Override
	protected Car update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(PROTOCOL)) proto = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) address = Integer.parseInt(params.get(ADDRESS));
		return this;
	}
}
