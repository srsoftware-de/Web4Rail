package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;

public class Locomotive extends Car implements Constants,Device{
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	private static final Integer CV_ADDR = 1;
	private static final String CVS = "cvs";
	private static final String ACTION_PROGRAM = "program";
	private static final String CV = "cv";
	private static final String VALUE = "val";
	private static final String MODE = "mode";
	private static final String POM = "pom";
	private static final String TRACK = "track";
	private Protocol proto = Protocol.DCC128;
	private int address = 3;
	private int speed = 0;
	private boolean f1,f2,f3,f4;
	private boolean init = false;
	private TreeMap<Integer,Integer> cvs = new TreeMap<Integer, Integer>();

	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, Id id) {
		super(name,id);
	}
	
	public static Object action(HashMap<String, String> params, Plan plan) throws IOException {
		String id = params.get(ID);
		Locomotive loco = id == null ? null : BaseClass.get(new Id(id));
		switch (params.get(ACTION)) {
			case ACTION_ADD:
				new Locomotive(params.get(Locomotive.NAME)).parent(plan).register();
				return Locomotive.manager();
			case ACTION_FASTER10:
				return loco.faster(10);
			case ACTION_MOVE:
				return loco.moveUp();
			case ACTION_PROGRAM:
				return loco.update(params);
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
		boolean fun1=false,fun2=false,fun3=false,fun4=false;
		if (locoOrTrain instanceof Locomotive) {
			loco = (Locomotive) locoOrTrain; 
			realm = REALM_LOCO;
			id = loco.id();
			speed = loco.speed;
			fun1 = loco.f1;
			fun2 = loco.f2;
			fun3 = loco.f3;
			fun4 = loco.f4;
		} else if (locoOrTrain instanceof Train) {
			train = (Train)locoOrTrain;
			realm = REALM_TRAIN;			
			id = train.id();
			speed = train.speed;
			fun1 = train.getFunction(1);
			fun2 = train.getFunction(2);
			fun3 = train.getFunction(3);
			fun4 = train.getFunction(4);
		}
		
		HashMap<String,Object> params = new HashMap<String, Object>(Map.of(REALM,realm,ID,id));
		
		Fieldset fieldset = new Fieldset(t("Control"));
		fieldset.clazz("cockpit");
		
		new Tag("span").content(t("Current velocity: {} {}",speed,speedUnit)).addTo(fieldset);
		
		Tag par = new Tag("p");
		
		params.put(ACTION, ACTION_FASTER10);
		new Button(t("Faster (10 {})",speedUnit),params).addTo(par);			

		params.put(ACTION, ACTION_SLOWER10);
		new Button(t("Slower (10 {})",speedUnit),params).addTo(par);			
		
		par.addTo(fieldset);

		Tag direction = new Tag("p");
		if ((isSet(train) && train.speed > 0) || (isSet(loco) && loco.speed > 0)) {
			params.put(ACTION, ACTION_STOP);
			new Button(t("Stop"),params).clazz(ACTION_STOP).addTo(direction);			
		}

		params.put(ACTION, ACTION_TURN);
		new Button(t("Turn"),params).title(t("Inverts the direction {} is heading to.",locoOrTrain)).clazz(ACTION_TURN).addTo(direction);			

		if (isSet(train)) {
			Block currentBlock = train.currentBlock();
			if (isSet(currentBlock)) {
				if (isNull(train.route())) {
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
		
		params.put(ACTION, ACTION_TOGGLE_F1);
		Button b1 = new Button(t("F1"),params);
		if (fun1) b1.clazz("active");
		b1.addTo(functions);

		params.put(ACTION, ACTION_TOGGLE_F2);
		Button b2 = new Button(t("F2"),params);
		if (fun2) b2.clazz("active");
		b2.addTo(functions);
		
		params.put(ACTION, ACTION_TOGGLE_F3);
		Button b3 = new Button(t("F3"),params);
		if (fun3) b3.clazz("active");
		b3.addTo(functions);

		params.put(ACTION, ACTION_TOGGLE_F4);
		Button b4 = new Button(t("F4"),params);
		if (fun4) b4.clazz("active");
		b4.addTo(functions);
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
		loco.put(REVERSE, orientation);
		loco.put(PROTOCOL, proto);
		json.put(LOCOMOTIVE, loco);
		loco.put(CVS, cvs);
		return json;
	}
	
	@Override
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(REVERSE)) orientation = loco.getBoolean(REVERSE);
			if (loco.has(PROTOCOL)) proto = Protocol.valueOf(loco.getString(PROTOCOL));
			if (loco.has(ADDRESS)) setAddress(loco.getInt(ADDRESS));
			if (loco.has(CVS)) {
				JSONObject jCvs = loco.getJSONObject(CVS);
				for (String key : jCvs.keySet()) cvs.put(Integer.parseInt(key),jCvs.getInt(key));
				address = cvs.get(CV_ADDR);
			}
		}
		return this;
	}	

	public static Window manager() {
		Window win = new Window("loco-manager", t("Locomotive manager"));
		new Tag("h4").content(t("known locomotives")).addTo(win);
		
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Protocol"),t("Address"),t("Length"),t("Tags"));
		List<Locomotive> locos = BaseClass.listElements(Locomotive.class);
		locos.sort(Comparator.comparing(loco -> loco.address));
		locos.sort(Comparator.comparing(loco -> loco.stockId));
		for (Locomotive loco : locos) {
			String maxSpeed = (loco.maxSpeedForward == 0 ? "–":""+loco.maxSpeedForward)+NBSP;
			if (loco.maxSpeedReverse != loco.maxSpeedForward) maxSpeed += "("+loco.maxSpeedReverse+")"+NBSP;
			table.addRow(loco.stockId,loco.link(),maxSpeed+speedUnit,loco.proto,loco.address,loco.length+NBSP+lengthUnit,String.join(", ", loco.tags()));
		}
		table.addTo(win);

		
		Form form = new Form("add-loco-form");
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_LOCO).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new locomotive"));
		new Input(Locomotive.NAME, t("new locomotive")).addTo(new Label(t("Name")+COL)).addTo(fieldset);
		new Button(t("Apply"),form).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}
	
	private String program(int cv,int val,boolean pom) {
		if (cv != 0) {			
			if (val < 0) {
				cvs.remove(cv);
				return null;
			}
			init();
			Command command = new Command("SET {} SM "+(pom?address:-1)+" CV "+cv+" "+val);
			try {
				Reply reply = plan.queue(command).reply();
				if (reply.succeeded()) {
					cvs.put(cv, val);
					if (cv == CV_ADDR) address = val;
					return null;
				}
				return reply.message();				
			} catch (TimeoutException e) {
				return t("Timeout while sending programming command!");		
			}	
		}
		return null;
	}
	
	private Fieldset programming() {
		Fieldset fieldset = new Fieldset(t("Programming"));

		Form form = new Form("cv-form");
		new Input(REALM,REALM_LOCO).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_PROGRAM).hideIn(form);

		Table table = new Table();
		table.addHead(t("setting"),t("CV"),t("value"),t("actions"));
		for (Entry<Integer, Integer> entry : cvs.entrySet()){
			int cv = entry.getKey();
			int val = entry.getValue();
			table.addRow(setting(cv),cv,val,new Button(t("edit"), "copyCv(this);"));
		}
		Tag mode = new Tag("div");
		new Radio(MODE, POM, t("program on main"), true).addTo(mode);
		new Radio(MODE, TRACK, t("prgramming track"), false).addTo(mode);
		table.addRow(mode,new Input(CV,0).numeric(),new Input(VALUE,0).numeric(),new Button(t("Apply"),form));
		return table.addTo(form).addTo(fieldset);
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
		postForm.add(programming());
		return super.properties(preForm, formInputs, postForm);
	}

	private void queue() {
		int step = proto.steps * speed / (maxSpeedForward == 0 ? 100 : maxSpeedForward); 
		init();
		plan.queue(new Command("SET {} GL "+address+" "+(orientation == FORWARD ? 0 : 1)+" "+step+" "+proto.steps+" "+(f1?1:0)+" "+(f2?1:0)+" "+(f3?1:0)+" "+(f4?1:0)) {

			@Override
			public void onFailure(Reply reply) {
				super.onFailure(reply);
				plan.stream(t("Failed to send command to {}: {}",this,reply.message()));
			}			
		});
	}
	
	private Locomotive setAddress(int newAddress) {
		address = newAddress;
		cvs.put(CV_ADDR, newAddress);
		return this;
	}
	
	public String setFunction(int num, boolean active) {
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
		queue();
		return t("{} F{}",t(active?"Activated":"Deavtivated"),num);
	}

	/**
	 * Sets the speed of the locomotive to the given velocity in [plan.speedUnit]s
	 * @param newSpeed
	 * @return
	 */
	public String setSpeed(int newSpeed) {
		LOG.debug(this.detail()+".setSpeed({})",newSpeed);
		speed = newSpeed;
		if (speed > maxSpeedForward && maxSpeedForward > 0) speed = maxSpeed();
		if (speed < 0) speed = 0;
		
		queue();
		return t("Speed of {} set to {}.",this,speed);
	}
	
	private Object setting(int cv) {
		switch (cv) {
		case 1:
			return t("Address");
		case 2:
			return t("minimum starting voltage v<sub>min</sub>");
		case 3:
			return t("starting delay");
		case 4:
			return t("braking delay");
		case 5:
			return t("maximum speed v<sub>max</sub>");
		case 6:
			return t("mid speed v<sub>mid</sub>");
		case 8:
			return t("PWM rate");
		case 17:
		case 18:
			return t("extended address");
		}
		return "";
	}

	
	public Object stop() {
		setSpeed(0);
		return properties();
	}
	
	Object toggleFunction(int f) {
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
	
	public Object turn() {		
		stop();
		super.turn();
		plan.stream(t("Stopped and reversed {}.",this));
		return properties();
	}
	
	@Override
	protected Window update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(PROTOCOL)) proto = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) {
			int newAddress = Integer.parseInt(params.get(ADDRESS));
			if (newAddress != address) {
				init = false;
				setAddress(newAddress);
			}
		}
		String error = null; 
		if (params.get(ACTION).equals(ACTION_PROGRAM)) try {
			int cv = Integer.parseInt(params.get(CV));
			int val = Integer.parseInt(params.get(VALUE));
			boolean pom = !params.get(MODE).equals(TRACK);
			error = program(cv,val,pom);
			
		} catch (NumberFormatException e) {}
		Window props = properties();
		if (isSet(error)) new Tag("span").content(error).addTo(props);
		return props;
	}
}
