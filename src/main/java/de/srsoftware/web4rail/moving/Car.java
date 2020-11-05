package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class Car extends BaseClass {
	protected static final Logger LOG = LoggerFactory.getLogger(Car.class);
	static HashMap<Integer,Car> cars = new HashMap<Integer, Car>();
	
	public static final String NAME = "name";
	private static final String LENGTH = "length";
	private static final String STOCK_ID = "stock-id";
	private static final String TAGS = "tags";
	protected HashSet<String> tags = new HashSet<String>();
	
	private int id;
	private String name;
	public int length;
	private String stockId = "";
	private Train train;
	protected Plan plan;
	
	public Car(String name) {
		this(name,null);
	}
	
	public Car(String name, Integer id) {
		this.name = name;
		if (id == null) {
			try { // make sure multiple consecutive creations get different ids
				Thread.sleep(1);
			} catch (InterruptedException e) {}
			id = Application.createId();
		}
		this.id = id;
		cars.put(id, this);
	}	
	
	public static Object action(HashMap<String, String> params,Plan plan) throws IOException {
		String id = params.get(ID);
		Car car = id == null ? null : Car.get(id);

		switch (params.get(ACTION)) {
			case ACTION_ADD:
				return new Car(params.get(Car.NAME)).plan(plan);

			case ACTION_PROPS:
				return car == null ? Car.manager() : car.properties();
			case ACTION_UPDATE:
				return car.update(params); 
		}
		if (car instanceof Locomotive) return Locomotive.action(params,plan);
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	public static Object manager() {
		Window win = new Window("car-manager", t("Car manager"));
		new Tag("h4").content(t("known cars")).addTo(win);
		Tag list = new Tag("ul");
		for (Car car : cars.values()) {
			if (!(car instanceof Locomotive)) {
				car.link("li").addTo(list);	
			}			
		}
		list.addTo(win);
		
		Form form = new Form();
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_CAR).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new car"));
		new Input(Locomotive.NAME, t("new car")).addTo(new Label(t("Name:")+" ")).addTo(fieldset);
		new Button(t("Apply")).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}

	protected Tag cockpit() {
		return null;
	}
	
	public static Car get(Object id) {
		return cars.get(Integer.parseInt(""+id)); // try to get by id
	}

	
	public int id() {
		return id;
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(ID,id);
		json.put(NAME, name);
		json.put(LENGTH, length);
		json.put(STOCK_ID, stockId);
		if (!tags.isEmpty()) json.put(TAGS, tags);
		return json;
	}
	
	public Tag link(String tagClass) {
		return link(tagClass, Map.of(REALM,REALM_CAR,ID,id,ACTION,ACTION_PROPS), name());
	}
	
	static Vector<Car> list() {
		Vector<Car> cars = new Vector<Car>();
		for (Car car : Car.cars.values()) {
			if (!(car instanceof Locomotive)) cars.add(car);
		}
		return cars;
	}
	
	public static void loadAll(String filename, Plan plan) throws IOException {
		cars.clear();
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			String name = json.getString(Car.NAME);
			int id = json.getInt(ID);
			Car car = json.has(Locomotive.LOCOMOTIVE) ? new Locomotive(name, id) : new Car(name,id);
			car.load(json).plan(plan);
			
			line = file.readLine();
		}
		file.close();
	}
	
	protected Car load(JSONObject json) {
		if (json.has(ID)) id = json.getInt(ID);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		if (json.has(STOCK_ID)) stockId = json.getString(STOCK_ID);
		if (json.has(TAGS)) json.getJSONArray(TAGS).forEach(elem -> { tags.add(elem.toString()); });
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
		new Input(NAME,name).addTo(new Label(t("Name")+NBSP)).addTo(fieldset);
		new Input(STOCK_ID,stockId).addTo(new Label(t("Stock ID")+NBSP)).addTo(fieldset);
		new Input(LENGTH,length).attr("type", "number").addTo(new Label(t("Length")+NBSP)).addTo(fieldset);
		new Input(TAGS,String.join(", ", tags)).addTo(new Label(t("Tags")+NBSP)).addTo(fieldset);
		fieldset.addTo(form);
		return form;
	}
	
	public Object properties() {
		Window win = new Window("car-props", t("Properties of {}",this));
		
		Tag cockpit = cockpit();
		if (cockpit != null) cockpit.addTo(win);
		
		Tag form = propertyForm();
		if (form!=null && form.children().size()>2) {
			new Button(t("Apply")).addTo(form);
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
		for (Entry<Integer, Car> entry: cars.entrySet()) file.write(entry.getValue().json()+"\n");
		file.close();
	}
	
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}
	
	public TreeSet<String> tags() {
		return new TreeSet<String>(tags);
	}
	
	@Override
	public String toString() {
		return name;
	}

	public void train(Train train) {
		this.train = train;
	}

	public Car update(HashMap<String, String> params) {
		if (params.containsKey(NAME)) name = params.get(NAME);
		if (params.containsKey(STOCK_ID)) stockId  = params.get(STOCK_ID);
		if (params.containsKey(LENGTH)) length = Integer.parseInt(params.get(LENGTH));
		if (params.containsKey(TAGS)) {
			String[] parts = params.get(TAGS).replace(",", " ").split(" ");
			tags.clear();
			for (String tag : parts) {
				tag = tag.trim();
				if (!tag.isEmpty()) tags.add(tag);
			}
		}
		return this;
	}
}
