package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class Car implements Constants {
	protected static final Logger LOG = LoggerFactory.getLogger(Car.class);
	static HashMap<String,Car> cars = new HashMap<String, Car>();
	
	public static final String NAME = "name";
	private static final String LENGTH = "length";
	private static final String STOCK_ID = "stock-id";
	
	private String id;
	private String name;
	public int length;
	private String stockId = "";
	private Train train;
	protected Plan plan;
	
	public Car(String name) {
		this(name,null);
	}
	
	public Car(String name, String id) {
		this.name = name;
		if (id == null) {
			try { // make sure multiple consecutive creations get different ids
				Thread.sleep(1);
			} catch (InterruptedException e) {}
			id = ""+new Date().getTime();
		}
		this.id = id;
		cars.put(id, this);
	}
	
	protected Tag cockpit(String realm) {
		return null;
	}
	
	public static Car get(String nameOrId) {
		Car car = cars.get(nameOrId); // try to get by id
		if (car == null) { // try to get by name
			for (Car c : cars.values()) {
				if (c.name.equals(nameOrId)) car = c;
			}
		}
		return car;
	}

	
	public String id() {
		return id;
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(ID,id);
		json.put(NAME, name);
		json.put(LENGTH, length);
		json.put(STOCK_ID, stockId);
		return json;
	}
	
	public Tag link(String tagClass) {
		return new Tag(tagClass).clazz("link").attr("onclick","car("+id+",'"+ACTION_PROPS+"')").content(name());
	}
	
	public static void loadAll(String filename, Plan plan) throws IOException {
		cars.clear();
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			String name = json.getString(Car.NAME);
			String id = json.getString(ID);
			Car car = json.has(Locomotive.LOCOMOTIVE) ? new Locomotive(name, id) : new Car(name,id);
			car.load(json).plan(plan);
			
			line = file.readLine();
		}
		file.close();
	}
	
	protected Car load(JSONObject json) {
		if (json.has(ID)) id = json.getString(ID);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		if (json.has(STOCK_ID)) stockId = json.getString(STOCK_ID);
		return this;
	}
	
	String name(){
		return name;
	}
	
	public Car plan(Plan plan) {
		this.plan = plan;
		return this;
	}
	
	public Tag propertyForm() {
		Form form = new Form();
		new Input(ACTION, ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_CAR).hideIn(form);
		new Input(ID,id()).hideIn(form);
		Fieldset fieldset = new Fieldset("Basic properties");
		new Input(NAME,name).addTo(new Label(t("Name"))).addTo(fieldset);
		new Input(STOCK_ID,stockId).addTo(new Label(t("Stock ID"))).addTo(fieldset);
		new Input(LENGTH,length).attr("type", "number").addTo(new Label(t("Length"))).addTo(fieldset);
		fieldset.addTo(form);
		return form;
	}
	
	public Object properties() {
		Window win = new Window("car-props", t("Properties of {}",this));
		
		Tag cockpit = cockpit("car");
		if (cockpit != null) cockpit.addTo(win);
		
		Tag form = propertyForm();
		if (form!=null && form.children().size()>2) {
			new Button(t("save")).addTo(form);
			form.addTo(win);
		} else {
			win.content(t("This tile ({}) has no editable properties",getClass().getSimpleName()));
		}
		
		Tag list = new Tag("ul");
		if (train != null) {
			train.link("span").addTo(new Tag("li").content(t("Train:")+" ")).addTo(list);
		}
		list.addTo(win);
		return win;
	}

	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<String, Car> entry: cars.entrySet()) {
			Car c = entry.getValue();
			file.write(c.json()+"\n");
		}
		file.close();
	}
	
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+name()+")";
	}

	public void train(Train train) {
		this.train = train;
	}

	public Object update(HashMap<String, String> params) {
		if (params.containsKey(NAME)) name = params.get(NAME);
		if (params.containsKey(STOCK_ID)) stockId  = params.get(STOCK_ID);
		if (params.containsKey(LENGTH)) length = Integer.parseInt(params.get(LENGTH));
		return null;
	}
}
