package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

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

public class Locomotive extends Car implements Constants,Device{
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	private static final int VMAX = 128;
	private boolean reverse = false;
	private Protocol proto = Protocol.DCC128;
	private int address = 3;
	private int speed = 0;
	private boolean f1,f2,f3,f4;
	private boolean init = false;

	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, Integer id) {
		super(name,id);
	}
	
	public static Object action(HashMap<String, String> params, Plan plan) throws IOException {
		String id = params.get(ID);
		Locomotive loco = id == null ? null : Locomotive.get(id);
		switch (params.get(ACTION)) {
			case ACTION_ADD:
				return new Locomotive(params.get(Locomotive.NAME)).plan(plan);
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
		}
		
		return t("Unknown action: {}",params.get(ACTION));
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

	protected Tag cockpit() {
		Fieldset fieldset = new Fieldset(t("Control"));
		String request = "return request({realm:'"+REALM_LOCO+"',id:"+id()+",action:'{}'})";
		new Button(t("Turn"), request.replace("{}", ACTION_TURN)).addTo(fieldset);
		new Button(t("Faster (10 steps)"), request.replace("{}", ACTION_FASTER10)).addTo(fieldset);
		new Button(t("Slower (10 steps)"), request.replace("{}", ACTION_SLOWER10)).addTo(fieldset);
		new Button(t("Stop"), request.replace("{}", ACTION_STOP)).addTo(fieldset);
		Tag span = new Tag("p");
		new Button(t("F1"),request.replace("{}", ACTION_TOGGLE_F1)).addTo(span);
		new Button(t("F2"),request.replace("{}", ACTION_TOGGLE_F2)).addTo(span);
		new Button(t("F3"),request.replace("{}", ACTION_TOGGLE_F3)).addTo(span);
		new Button(t("F4"),request.replace("{}", ACTION_TOGGLE_F4)).addTo(span);
		span.addTo(fieldset);
		return fieldset;
	}
	
	private String detail() {
		return getClass().getSimpleName()+"("+name()+", "+proto+", "+address+")";
	}

	
	public Object faster(int steps) {
		return setSpeed(speed + steps);
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
			proto = "N 1 14 5"; break; // TODO: make configurable
		case DCC27:
			proto = "N 1 27 5"; break; // TODO: make configurable
		case DCC28:
			proto = "N 1 28 5"; break; // TODO: make configurable
		case DCC128:
			proto = "N 1 128 5"; break; // TODO: make configurable
		case SELECTRIX:
			proto = "S"; break;
		case ZIMO:
			proto = "Z"; break;
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
	
	static Vector<Car> list() {
		Vector<Car> locos = new Vector<Car>();
		for (Car car : Car.cars.values()) {
			if (car instanceof Locomotive) locos.add((Locomotive) car);
		}
		return locos;
	}

	@Override
	protected Car load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(REVERSE)) reverse = loco.getBoolean(REVERSE);
			if (loco.has(PROTOCOL)) proto = Protocol.valueOf(loco.getString(PROTOCOL));
			if (loco.has(ADDRESS)) address = loco.getInt(ADDRESS);
		}
		return this;
	}	

	public static Object manager() {
		Window win = new Window("loco-manager", t("Locomotive manager"));
		new Tag("h4").content(t("known locomotives")).addTo(win);
		Tag list = new Tag("ul");
		for (Car car : cars.values()) {
			if (car instanceof Locomotive) {
				Locomotive loco = (Locomotive) car;
				Tag tag = loco.link("li");
				if (isSet(loco.stockId) && !loco.stockId.isEmpty()) tag.content(NBSP+t("(id: {}, length: {})",loco.stockId,loco.length));
				tag.addTo(list);	

			}			
		}
		list.addTo(win);
		
		Form form = new Form();
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_LOCO).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new locomotive"));
		new Input(Locomotive.NAME, t("new locomotive")).addTo(new Label(t("Name:")+NBSP)).addTo(fieldset);
		new Button(t("Apply")).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public Tag propertyForm() {
		Tag form = super.propertyForm();
		for (Tag tag : form.children()) {
			if (REALM.equals(tag.get(Input.NAME)) && REALM_CAR.equals(tag.get(Input.VALUE))) {
				tag.attr(REALM, REALM_LOCO);
				break;
			}
		}
		Fieldset fieldset = new Fieldset("Decoder settings");
		Label protocol = new Label(t("Protocol:"));
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.proto).addTo(protocol);
		}
		protocol.addTo(fieldset);
		new Input(ADDRESS, address).attr("type", "number").addTo(new Label(t("Address:"))).addTo(fieldset);
		fieldset.addTo(form);
		return form;
	}
	
	private void queue() {
		plan.queue(new Command("SET {} GL "+address+" "+(reverse?1:0)+" "+speed+" "+VMAX+" "+(f1?1:0)+" "+(f2?1:0)+" "+(f3?1:0)+" "+(f4?1:0)) {

			@Override
			public void onFailure(Reply reply) {
				super.onFailure(reply);
				plan.stream(t("Failed to send command to {}: {}",this,reply.message()));
			}			
		});
	}

	public String setSpeed(int newSpeed) {
		LOG.debug(this.detail()+".setSpeed({})",newSpeed);
		init();
		speed = newSpeed;
		if (speed > 128) speed = 128;
		if (speed < 0) speed = 0;
		
		queue();
		return t("Speed of {} set to {}.",this,speed);
	}
	
	public Object stop() {
		setSpeed(0);
		return t("Stopped {}",this);
	}
	
	public Object turn() {
		reverse = !reverse;
		stop();
		return t("Stopped and reversed {}.",this);
	}
	
	@Override
	public Car update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(PROTOCOL)) proto = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) address = Integer.parseInt(params.get(ADDRESS));
		return this;
	}
}
