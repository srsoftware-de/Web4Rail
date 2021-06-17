package de.srsoftware.web4rail.moving;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.functions.Function;
import de.srsoftware.web4rail.functions.FunctionList;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Range;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;

public class Locomotive extends Car implements Constants{
	public static final String LOCOMOTIVE = "locomotive";
	private static final String ACTION_MAPPING = "mapping";
	private FunctionList functions = new FunctionList(); 
	private int speed = 0;
	//private TreeMap<Integer,Integer> cvs = new TreeMap<Integer, Integer>();
	private Decoder decoder;
	
	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, Id id) {
		super(name,id);
	}
	
	public static Object action(Params params, Plan plan) throws IOException {
		String id = params.getString(ID);
		Locomotive loco = id == null ? null : BaseClass.get(new Id(id));
		switch (params.getString(ACTION)) {
			case ACTION_ADD:
				new Locomotive(params.getString(Locomotive.NAME)).parent(plan).register();
				return Locomotive.manager();
			case ACTION_FASTER10:
				return loco.faster(Train.defaultSpeedStep);
			case ACTION_MAPPING:
				return loco.updateMapping(params);
			case ACTION_MOVE:
				return loco.moveUp();
			case ACTION_PROPS:
				return loco == null ? Locomotive.manager() : loco.properties();
			case ACTION_SET_SPEED:
				return loco.setSpeed(params.getInt(SPEED));
			case ACTION_SLOWER10:
				return loco.faster(-Train.defaultSpeedStep);
			case ACTION_STOP:
				return loco.stop();
			case ACTION_TOGGLE_FUNCTION:
				return loco.toggleFunction(params);
			case ACTION_TURN:
				return loco.turn();
			case ACTION_UPDATE:
				loco.update(params);
				return Locomotive.manager();
		}
		
		String message = t("Unknown action: {}",params.getString(ACTION));
		return (isNull(loco)) ? message : loco.properties(message);
	}
	
	private void addDecoderButtons(Window props) {
		Tag basicProps = props.children().stream().filter(tag -> BaseClass.PROPS_BASIC.equals(tag.get("id"))).findFirst().get();
		Tag form = basicProps.children().stream().filter(tag -> tag.is("form")).findFirst().get();
		Table table = (Table) form.children().stream().filter(tag -> tag.is("table")).findFirst().get();
		Tag div = new Tag("div");
		if (isSet(decoder)) {
			decoder.button().addTo(div);
			decoder.button(t("dismount"), Map.of(ACTION,ACTION_DECOUPLE)).addTo(div);
		} else {
			Decoder.selector(true).addTo(div);
		}
		table.addRow(t("Decoder"),div);
		Vector<Tag> cols = table.children();
		Tag lastRow = cols.lastElement();
		cols.remove(cols.size()-1);
		cols.insertElementAt(lastRow, 5);
	}

	
	public static Fieldset cockpit(BaseClass locoOrTrain) {
		int speed = 0;
		String realm = null;
		final Train train;
		final Locomotive loco;
		int maxSpeed = 0;
		String id = null;
		if (locoOrTrain instanceof Locomotive) {
			loco = (Locomotive) locoOrTrain;
			train = null;
			realm = REALM_LOCO;
			speed = loco.speed;
			maxSpeed = loco.orientation ? loco.maxSpeedForward : loco.maxSpeedReverse;
			id = "loco_"+loco.id();
		} else if (locoOrTrain instanceof Train) {
			loco = null;
			train = (Train)locoOrTrain;
			realm = REALM_TRAIN;			
			speed = train.speed;
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

		if (isSet(loco)) {
			loco.functionNames().forEach(name -> {
				Button btn = loco.button(name, Map.of(ACTION,ACTION_TOGGLE_FUNCTION,FUNCTION,name));
				if (loco.functions.enabled(name)) btn.clazz("active");
				btn.addTo(functions);	
			});
		}
		
		if (isSet(train)) {
			train.functionNames().sorted().forEach(name -> {
				Button btn = train.button(name, Map.of(ACTION,ACTION_TOGGLE_FUNCTION,FUNCTION,name));
				if (train.functionEnabled(name)) btn.clazz("active");
				btn.addTo(functions);	
				
			});
		}

		functions.addTo(fieldset);
		
		if (isSet(train)) {
			train.button(t("Select destination"),Map.of(ACTION,ACTION_MOVE,ASSIGN,DESTINATION)).addTo(direction);
			Button toggleShunbting = train.button(t("Shunting"),Map.of(ACTION,ACTION_TOGGLE_SHUNTING));
			if (train.isShunting()) toggleShunbting.clazz(toggleShunbting.get("class")+" active");
			toggleShunbting.addTo(functions);
		}

		return fieldset;
	}
	
	public Decoder decoder() {
		return decoder;
	}
	
	private String detail() {
		return getClass().getSimpleName()+"("+name()+", "+decoder.protocol()+", "+decoder.address()+")";
	}

	
	public Tag faster(int steps) {
		return setSpeed(speed + steps);		
	}
	
	private Fieldset functionMapping() {
		Fieldset fieldset = new Fieldset(t("Function mapping")).id("props-functions");
		Form form = new Form("function-mapping");
		new Input(REALM, REALM_LOCO).hideIn(form);
		new Input(ACTION, ACTION_MAPPING).hideIn(form);
		new Input(ID,id()).hideIn(form);
		
		functions.stream()
			.sorted((a,b) -> a.index() - b.index())
			.forEach(fun -> fun.button(t("delete"), Map.of(ACTION,ACTION_DROP)).addTo(fun.form(decoder)).addTo(form));
		Fieldset newFun = new Fieldset(t("Add function"));
		Function.selector().addTo(newFun).addTo(form);
		
		return new Button(t("Save"), form).addTo(form).addTo(fieldset);
	}
	
	public Stream<String> functionNames() {
		return functions.stream().map(Function::name).sorted().distinct();
	}

	public FunctionList functions() {
		return functions;
	}
	
	public HashSet<Function> functions(String name){
		return functions.with(name);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONObject loco = new JSONObject();
		json.put(LOCOMOTIVE, loco);
		if (isSet(decoder))	loco.put(Decoder.DECODER,decoder.json());
		if (functions.size()>0) loco.put(FUNCTIONS,functions.json());
		return json;
	}
	
	@Override
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(Decoder.DECODER)) {
				if (isNull(decoder)) decoder = new Decoder();
				decoder.load(loco.getJSONObject(Decoder.DECODER));
			}
			if (loco.has(Decoder.CVS)) { // Legacy
				if (isNull(decoder)) decoder = new Decoder();
				decoder.register();
				JSONObject jCvs = loco.getJSONObject(Decoder.CVS);
				for (String key : jCvs.keySet()) decoder.cvs.put(Integer.parseInt(key),jCvs.getInt(key));
			}
			if (isSet(decoder)) decoder.setLoco(this,false);
			
			if (loco.has(FUNCTIONS)) functions.load(loco.getJSONArray(FUNCTIONS),this);
			
		}
		return this;
	}	

	public static Window manager() {
		Window win = new Window("loco-manager", t("Locomotive manager"));
		new Tag("h4").content(t("known locomotives")).addTo(win);
		
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Address"),t("Decoder"),t("Length"),t("driven distance"),t("Tags"));
		List<Locomotive> locos = BaseClass.listElements(Locomotive.class);
		locos.sort(Comparator.comparing(loco -> isSet(loco.decoder) ? loco.decoder.address() : 0));
		locos.sort(Comparator.comparing(loco -> loco.stockId));
		for (Locomotive loco : locos) {
			String maxSpeed = (loco.maxSpeedForward == 0 ? "–":""+loco.maxSpeedForward)+NBSP;
			if (loco.maxSpeedReverse != loco.maxSpeedForward) maxSpeed += "("+loco.maxSpeedReverse+")"+NBSP;
			table.addRow(loco.stockId,
				loco.link(),
				maxSpeed+NBSP+speedUnit,
				isSet(loco.decoder) ? loco.decoder.address() : null,
				isSet(loco.decoder) ? loco.decoder.button() : null,
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
		if (isSet(decoder) && decoder.numFunctions()>0)	postForm.add(functionMapping());
		Window props = super.properties(preForm, formInputs, postForm,errors);
		addDecoderButtons(props);
		return props;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		functions.remove(child);
		super.removeChild(child);
	}
	
	public void removeDecoder(Decoder decoder) {
		if (this.decoder == decoder) {
			addLogEntry(t("Removed decoder \"{}\".",decoder));
			this.decoder = null;
		}
	}
	
	public void setDecoder(Decoder newDecoder, boolean log) {
		decoder = newDecoder;
		if (log) addLogEntry(t("Mounted decoder \"{}\".",decoder));
	}
	
	/**
	 * Sets the speed of the locomotive to the given velocity in [plan.speedUnit]s
	 * @param newSpeed
	 * @return
	 */
	public Tag setSpeed(int newSpeed) {
		if (isSet(decoder)) {
			LOG.debug(this.detail()+".setSpeed({})",newSpeed);
			speed = newSpeed;
			if (speed > maxSpeedForward && maxSpeedForward > 0) speed = maxSpeed();
			if (speed < 0) speed = 0;
			
			double step = 1.0 * speed / (maxSpeedForward == 0 ? 100 : maxSpeedForward); 
			decoder.setSpeed(step,orientation != FORWARD);
		}
		return properties();
		
	}


	
	public Object stop() {
		setSpeed(0);
		return properties();
	}
	
	private Window toggleFunction(Params params) {
		functions.toggle(params.getString(FUNCTION));
		if (isSet(decoder)) decoder.queue();
		return properties();
	}
	
	public Object turn() {		
		setSpeed(0);
		super.turn();
		plan.stream(t("Stopped and reversed {}.",this));
		return stop();
	}
	
	@Override
	protected Window update(Params params) {
		super.update(params);
		if (params.containsKey(REALM_DECODER)) {
			Id decoderId = Id.from(params,REALM_DECODER);
			Decoder decoder = null;
			switch (decoderId.toString()) {
				case "-1":
					break;
				case "0":
					decoder = new Decoder().register();
					break;
				default:
					decoder = Decoder.get(decoderId);
			}
			if (isSet(decoder))	decoder.setLoco(this,true);
		}
		return properties();
	}
	
	private Object updateMapping(Params params) {
		if (params.containsKey(Function.NEW)) {
			String type = params.getString(Function.NEW);
			if (!type.isEmpty()) {
				Function newFun = Function.create(type);
				if (isSet(newFun)) functions.add(newFun.register());
			}
		}
		if (params.containsKey(FUNCTIONS)) {
			Params funs = params.getParams(FUNCTIONS);
			for (String id : funs.keySet()) {
				Function function = BaseClass.get(new Id(id));
				if (isSet(function)) function.update(funs.getParams(id));
			}
		}
		if (isSet(decoder)) decoder.queue();
		return properties();
	}
}
