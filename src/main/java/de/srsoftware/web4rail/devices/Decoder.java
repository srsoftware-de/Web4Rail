package de.srsoftware.web4rail.devices;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;

public class Decoder extends BaseClass implements Constants, Device {
	
	public static final String DECODER = "decoder";
	public static final String CVS = "cvs";
	private Integer address = null;
	public TreeMap<Integer,Integer> cvs = new TreeMap<Integer, Integer>();
	private boolean init = false;
	private Protocol proto = Protocol.DCC128;
	public static final Integer CV_ADDR = 1;
	private static final String ACTION_PROGRAM = "program";
	private static final String MODE = "mode";
	private static final String POM = "pom";
	private static final String TRACK = "track";
	private static final String VALUE = "val";
	private static final String CV = "cv";
	private static final String NUM_FUNCTIONS = "numFunctions";
	private String type;
	private Locomotive loco;
	private int numFunctions = 2;

	public static Object action(Params params) {
		Decoder decoder = BaseClass.get(Id.from(params));
		switch (params.getString(Constants.ACTION)) {
			case ACTION_DECOUPLE:
				return decoder.dismount();
			case ACTION_PROGRAM:
				return decoder.program(params);
			case ACTION_PROPS:
				return decoder.properties();
			case ACTION_UPDATE:
				return decoder.update(params);
		}
		
		String message = BaseClass.t("Unknown action: {}",params.getString(Constants.ACTION));
		return (BaseClass.isNull(decoder)) ? message : decoder.properties(message);
	}
	
	private Window dismount() {
		if (isNull(loco)) return properties();
		Locomotive locomotive = loco;
		locomotive.removeDecoder(this);
		loco = null;
		addLogEntry(t("Removed decoder from \"{}\".",locomotive));
		return locomotive.properties();
	}

	@Override
	public int address() {
		if (isNull(address)) address = cvs.get(CV_ADDR);
		return isNull(address) ? 3 : address;
	}
	
	public Button button() {
		return super.button(type(),Map.of(REALM,REALM_DECODER));
	}
	
	public void init() {
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
		plan.queue(new Command("INIT {} GL "+address()+" "+proto) {
			
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
		json.put(CVS, cvs);
		json.put(PROTOCOL, proto);
		json.put(TYPE, type);
		return json;
	}
	
	@Override
	public Tag link(String... args) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Decoder load(JSONObject json) {
		super.load(json);
		if (json.has(PROTOCOL)) proto = Protocol.valueOf(json.getString(PROTOCOL));
		if (json.has(CVS)) { // Legacy
			JSONObject jCvs = json.getJSONObject(CVS);
			for (String key : jCvs.keySet()) cvs.put(Integer.parseInt(key),jCvs.getInt(key)); 
		}
		if (json.has(TYPE)) type = json.getString(TYPE);
		return this;
	}
	
	public int numFunctions() {
		return numFunctions ;
	}





	private Window program(Params params) {
		String error = null;
		if (ACTION_PROGRAM.equals(params.get(ACTION))) try {
			int cv = params.getInt(CV);
			int val = params.getInt(VALUE);
			boolean pom = !TRACK.equals(params.get(MODE));
			error = program(cv,val,pom);			
		} catch (NumberFormatException e) {}
		return properties(error);
	}
	
	private String program(int cv,int val,boolean pom) {
		if (cv != 0) {
			if (val < 0) {
				cvs.remove(cv);
				return null;
			}
			init();
			Command command = new Command("SET {} SM "+(pom?address():-1)+" CV "+cv+" "+val);
			try {
				Reply reply = plan.queue(command).reply();
				if (reply.succeeded()) {
					cvs.put(cv, val);
					address = cvs.get(CV_ADDR); // update address field:
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
		Fieldset fieldset = new Fieldset(t("Programming")).id("props-cv");

		Form form = new Form("cv-form");
		new Input(REALM,REALM_DECODER).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_PROGRAM).hideIn(form);

		Table table = new Table();
		table.addHead(t("setting"),t("CV"),t("value"),t("actions"));
		for (int cv=1; cv<19; cv++) {
			Object val = cvs.get(cv);
			if (isNull(val)) {
				if (Set.of(7, 8, 10, 11, 12, 13, 14, 15, 16).contains(cv)) continue;
				val = t("no value");
			}
			table.addRow(setting(cv),cv,val,new Button(t("edit"), "copyCv(this);"));
		}
		for (Entry<Integer, Integer> entry : cvs.entrySet()){			
			int cv = entry.getKey();
			if (cv<10) continue;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm, String... errorMessages) {
		formInputs.add(t("Type"),new Input(TYPE,type()));
		Tag div = new Tag("div");
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.proto).addTo(div);
		}
		formInputs.add(t("Protocol"),div);
		formInputs.add(t("Address"),new Tag("span").content(""+address()));
		formInputs.add(t("Number of functions"),new Input(NUM_FUNCTIONS,numFunctions).numeric());
		if (isSet(loco)) formInputs.add(t("Locomotive"),loco.button(loco.name()));
		postForm.add(programming());
		return super.properties(preForm, formInputs, postForm, errorMessages);
	}

	public Protocol protocol() {
		return proto;
	}
	
	public static Select selector(boolean freeOnly) {
		Select selector = new Select(REALM_DECODER);
		List<Decoder> decoders = BaseClass.listElements(Decoder.class);
		selector.addOption(-1,t("no decoder"));
		selector.addOption(0,t("new decoder"));
		for (Decoder d: decoders) {
			if (freeOnly && isSet(d.loco)) continue;
			selector.addOption(d.id(), d);
		}
		return selector;
	}
	
	public Decoder setLoco(Locomotive locomotive, boolean log) {
		loco = locomotive;
		if (log) addLogEntry(t("Mounted into \"{}\".",loco));
		locomotive.setDecoder(this,log);
		return this;
	}
	
	public void setProtocol(Protocol proto) {
		this.proto = proto;
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
		case 9:
			return t("PWM rate");
		case 17:
		case 18:
			return t("extended address");
		}
		return "";
	}
	
	@Override
	public String toString() {
		return type()+" ("+t("Address")+": "+address()+")";
	}
	

	public String type() {
		return isSet(type) ? type : t("Unknown decoder type");
	}
	
	@Override
	protected Window update(Params params) {
		super.update(params);
		if (params.containsKey(TYPE)) type = params.getString(TYPE);
		if (params.containsKey(Device.PROTOCOL)) setProtocol(Protocol.valueOf(params.getString(Device.PROTOCOL)));





		if (params.containsKey(NUM_FUNCTIONS)) numFunctions = params.getInt(NUM_FUNCTIONS);





		return isSet(loco) ? loco.properties() : properties();
	}
}
