package de.srsoftware.web4rail.moving;

import java.util.HashMap;
import java.util.Vector;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;

public class Locomotive extends Car {
	
	public enum Protocol{
		DCC14,DCC27,DCC28,DCC128,MOTO;
	}
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	private static final String PROTOCOL = "protocol";
	private static final String ADDRESS = "address";
	private boolean reverse = false;
	private Protocol proto = Protocol.DCC128;
	private int address = 3;

	public Locomotive(String name) {
		super(name);
	}
	
	public Locomotive(String name, String id) {
		super(name,id);
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
	protected void load(JSONObject json) {
		super.load(json);
		if (json.has(LOCOMOTIVE)) {
			JSONObject loco = json.getJSONObject(LOCOMOTIVE);
			if (loco.has(REVERSE)) reverse = loco.getBoolean(REVERSE);
			if (loco.has(PROTOCOL)) proto = Protocol.valueOf(loco.getString(PROTOCOL));
			if (loco.has(ADDRESS)) address = loco.getInt(ADDRESS);
		}
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
