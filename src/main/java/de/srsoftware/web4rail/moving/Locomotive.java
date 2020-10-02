package de.srsoftware.web4rail.moving;

import java.util.HashMap;
import java.util.Vector;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
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

public class Locomotive extends Car implements Device{
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	private boolean reverse = false;
	private Protocol proto = Protocol.DCC128;
	private int address = 3;
	private boolean init = false;

	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, String id) {
		super(name,id);
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
		plan.queue("INIT {} GL "+address+" "+proto);
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
	
	static Vector<Locomotive> list() {
		Vector<Locomotive> locos = new Vector<Locomotive>();
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
				loco.link("li").addTo(list);	
			}			
		}
		list.addTo(win);
		
		Form form = new Form();
		new Input(Plan.ACTION, Plan.ACTION_ADD_LOCO).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new locomotive"));
		new Input(Locomotive.NAME, t("new locomotive")).addTo(new Label(t("Name:")+" ")).addTo(fieldset);
		new Button(t("save")).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public Tag propertyForm() {
		Tag form = super.propertyForm();
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

	public void setSpeed(int v) {
		init();
		plan.queue("SET {} GL "+address+" "+(reverse?1:0)+" "+v+" 128 0 0 0 0 0");
		LOG.debug("{}.setSpeed({})",this,v);
	}
	
	@Override
	public Object update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(PROTOCOL)) proto = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) address = Integer.parseInt(params.get(ADDRESS));
		return t("Updated locomotive.");
	}
}
