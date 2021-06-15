package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.devices.Device;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Range;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;

public class Locomotive extends Car implements Constants{
	
	public static final String LOCOMOTIVE = "locomotive";
	private int speed = 0;
	private boolean f1,f2,f3,f4;
	//private TreeMap<Integer,Integer> cvs = new TreeMap<Integer, Integer>();
	private Decoder decoder;

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
				return loco.faster(Train.defaultSpeedStep);
			case ACTION_MOVE:
				return loco.moveUp();
/*			case ACTION_PROGRAM:
				return loco.update(params); */
			case ACTION_PROPS:
				return loco == null ? Locomotive.manager() : loco.properties();
			case ACTION_SET_SPEED:
				return loco.setSpeed(Integer.parseInt(params.get(SPEED)));
			case ACTION_SLOWER10:
				return loco.faster(-Train.defaultSpeedStep);
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
	
	public static Fieldset cockpit(BaseClass locoOrTrain) {
		int speed = 0;
		String realm = null;
		Train train = null;
		Locomotive loco = null;
		int maxSpeed = 0;
		boolean fun1=false,fun2=false,fun3=false,fun4=false;
		String id = null;
		if (locoOrTrain instanceof Locomotive) {
			loco = (Locomotive) locoOrTrain; 
			realm = REALM_LOCO;
			speed = loco.speed;
			fun1 = loco.f1;
			fun2 = loco.f2;
			fun3 = loco.f3;
			fun4 = loco.f4;
			maxSpeed = loco.orientation ? loco.maxSpeedForward : loco.maxSpeedReverse;
			id = "loco_"+loco.id();
		} else if (locoOrTrain instanceof Train) {
			train = (Train)locoOrTrain;
			realm = REALM_TRAIN;			
			speed = train.speed;
			fun1 = train.getFunction(1);
			fun2 = train.getFunction(2);
			fun3 = train.getFunction(3);
			fun4 = train.getFunction(4);
			maxSpeed = train.maxSpeed();
			id = "train_"+train.id();
		} else return null;
		
		HashMap<String,Object> params = new HashMap<String, Object>(Map.of(REALM,realm,ID,locoOrTrain.id()));
		
		Fieldset fieldset = new Fieldset(t("Control")).id("props-cockpit");
		fieldset.clazz("cockpit");
		
		Tag par = new Tag("p");
		
		Range range = new Range(t("Current velocity: {} {}",speed,speedUnit),"speed",speed,0,maxSpeed);
		range.id(id).onChange("changeSpeed('"+id+"');");
		range.addTo(par);
		
		
		params.put(ACTION, ACTION_FASTER10);
		new Button(t("Faster ({} {})",Train.defaultSpeedStep,speedUnit),params).addTo(par);			

		params.put(ACTION, ACTION_SLOWER10);
		new Button(t("Slower ({} {})",Train.defaultSpeedStep,speedUnit),params).addTo(par);			
		
		par.addTo(fieldset);

		Tag direction = new Tag("p");
		if ((isSet(train) && train.isStoppable()) || (isSet(loco) && loco.speed > 0)) {
			params.put(ACTION, ACTION_STOP);
			new Button(t("Stop"),params).clazz(ACTION_STOP).addTo(direction);			
		}

		params.put(ACTION, ACTION_TURN);
		new Button(t("Turn"),params).title(t("Inverts the direction {} is heading to.",locoOrTrain)).clazz(ACTION_TURN).addTo(par);			

		if (isSet(train)) {
			Block currentBlock = train.currentBlock();
			if (isSet(currentBlock)) {
				if (isSet(train.direction()) && !train.isStoppable()) {
					params.put(ACTION, ACTION_START);
					new Button(t("depart"),params).addTo(direction);
				}
				if (train.usesAutopilot()) {
					params.put(ACTION, ACTION_QUIT);
					new Button(t("quit autopilot"),params).addTo(direction);
				} else if (isSet(train.direction())){
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
		
		if (isSet(train)) {
			train.button(t("Select destination"),Map.of(ACTION,ACTION_MOVE,ASSIGN,DESTINATION)).addTo(direction);
			Button toggleShunbting = train.button(t("Shunting"),Map.of(ACTION,ACTION_TOGGLE_SHUNTING));
			if (train.isShunting()) toggleShunbting.clazz(toggleShunbting.get("class")+" active");
			toggleShunbting.addTo(functions);
		}

		return fieldset;
	}
	
	private String detail() {
		return getClass().getSimpleName()+"("+name()+", "+decoder.protocol()+", "+decoder.address()+")";
	}

	
	public Tag faster(int steps) {
		return setSpeed(speed + steps);		
	}

	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONObject loco = new JSONObject();
		//loco.put(PROTOCOL, proto);
		json.put(LOCOMOTIVE, loco);
		//loco.put(CVS, cvs);
		loco.put(Decoder.DECODER,decoder.json());
		return json;
	}
	
	@Override
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			if (isNull(decoder)) decoder = new Decoder();
			
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(Decoder.DECODER)) decoder.load(json.getJSONObject(Decoder.DECODER));
			if (loco.has(Decoder.CVS)) { // Legacy
				JSONObject jCvs = loco.getJSONObject(Decoder.CVS);
				for (String key : jCvs.keySet()) {
					decoder.cvs.put(Integer.parseInt(key),jCvs.getInt(key)); 
				}
			}
		}
		return this;
	}	

	public static Window manager() {
		Window win = new Window("loco-manager", t("Locomotive manager"));
		new Tag("h4").content(t("known locomotives")).addTo(win);
		
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Protocol"),t("Address"),t("Length"),t("driven distance"),t("Tags"));
		List<Locomotive> locos = BaseClass.listElements(Locomotive.class);
		locos.sort(Comparator.comparing(loco -> isSet(loco.decoder) ? loco.decoder.address() : 0));
		locos.sort(Comparator.comparing(loco -> loco.stockId));
		for (Locomotive loco : locos) {
			String maxSpeed = (loco.maxSpeedForward == 0 ? "–":""+loco.maxSpeedForward)+NBSP;
			if (loco.maxSpeedReverse != loco.maxSpeedForward) maxSpeed += "("+loco.maxSpeedReverse+")"+NBSP;
			table.addRow(loco.stockId,
				loco.link(),
				maxSpeed+NBSP+speedUnit,
				isSet(loco.decoder) ? loco.decoder.protocol() : null,
				isSet(loco.decoder) ? loco.decoder.address() : null,
				loco.length+NBSP+lengthUnit,
				loco.distanceCounter,
				String.join(", ", loco.tags()));
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
	

	

	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		preForm.add(cockpit(this));
		Window props = super.properties(preForm, formInputs, postForm,errors);
		if (isSet(decoder)) {
			Tag basicProps = props.children().stream().filter(tag -> BaseClass.PROPS_BASIC.equals(tag.get("id"))).findFirst().get();
			Tag form = basicProps.children().stream().filter(tag -> tag.is("form")).findFirst().get();
			Table table = (Table) form.children().stream().filter(tag -> tag.is("table")).findFirst().get();
			table.addRow(t("Decoder"),decoder.link());
			Vector<Tag> cols = table.children();
			Tag lastRow = cols.lastElement();
			cols.remove(cols.size()-1);
			cols.insertElementAt(lastRow, 5);
		}
		return props;
	}
	
	private void queue() {
		if (isNull(decoder)) return;
		int step = decoder.protocol().steps * speed / (maxSpeedForward == 0 ? 100 : maxSpeedForward); 
		decoder.init();
		plan.queue(new Command("SET {} GL "+decoder.address()+" "+(orientation == FORWARD ? 0 : 1)+" "+step+" "+decoder.protocol().steps+" "+(f1?1:0)+" "+(f2?1:0)+" "+(f3?1:0)+" "+(f4?1:0)) {

			@Override
			public void onFailure(Reply reply) {
				super.onFailure(reply);
				plan.stream(t("Failed to send command to {}: {}",this,reply.message()));
			}			
		});
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
	public Tag setSpeed(int newSpeed) {
		LOG.debug(this.detail()+".setSpeed({})",newSpeed);
		speed = newSpeed;
		if (speed > maxSpeedForward && maxSpeedForward > 0) speed = maxSpeed();
		if (speed < 0) speed = 0;
		
		queue();
		//plan.stream(t("Speed of {} set to {} {}.",this,speed,BaseClass.speedUnit));
		return properties();
		
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
		setSpeed(0);
		super.turn();
		plan.stream(t("Stopped and reversed {}.",this));
		return stop();
	}
	
	@Override
	protected Window update(HashMap<String, String> params) {
		super.update(params);
		if (isSet(decoder) && params.containsKey(Device.PROTOCOL)) decoder.setProtocol(Protocol.valueOf(params.get(Device.PROTOCOL)));
		if (isSet(decoder) && params.containsKey(Device.ADDRESS)) {
			int newAddress = Integer.parseInt(params.get(Device.ADDRESS));
			if (newAddress != decoder.address()) decoder.cvs.put(Decoder.CV_ADDR, newAddress);
		}
		String error = null; 
		/*if (params.get(ACTION).equals(ACTION_PROGRAM)) try {
			int cv = Integer.parseInt(params.get(CV));
			int val = Integer.parseInt(params.get(VALUE));
			boolean pom = !params.get(MODE).equals(TRACK);
			error = program(cv,val,pom);
			
		} catch (NumberFormatException e) {}*/
		Window props = properties();
		if (isSet(error)) new Tag("span").content(error).addTo(props);
		return props;
	}
}
