package de.srsoftware.web4rail.moving;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import de.srsoftware.web4rail.tags.Table;

public class Car extends BaseClass implements Comparable<Car>{
	protected static final Logger LOG = LoggerFactory.getLogger(Car.class);
	static HashMap<Id,Car> cars = new HashMap<Id, Car>();
	
	public static final String NAME = "name";
	private static final String LENGTH = "length";
	private static final String STOCK_ID = "stock-id";
	private static final String TAGS = "tags";
	private static final String MAX_SPEED = "max_speed";
	protected HashSet<String> tags = new HashSet<String>();
	
	private String name;
	public int length;
	protected String stockId = "";
	private Train train;
	protected Plan plan;
	protected int maxSpeed = 0;
	
	public Car(String name) {
		this(name,null);
	}
	
	public Car(String name, Id id) {
		this.name = name;
		if (isNull(id)) id = new Id();
		this.id = id;
		cars.put(id, this);
	}	
	
	public static Object action(HashMap<String, String> params,Plan plan) throws IOException {
		String id = params.get(ID);
		Car car = id == null ? null : Car.get(id);

		switch (params.get(ACTION)) {
			case ACTION_ADD:
				if (isSet(car)) {
					car.clone().plan(plan);
				} else new Car(params.get(Car.NAME)).plan(plan);
				return Car.manager();
			case ACTION_PROPS:
				return car == null ? Car.manager() : car.properties();
			case ACTION_UPDATE:
				car.update(params);
				return Car.manager();
		}
		if (car instanceof Locomotive) return Locomotive.action(params,plan);
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	public Car clone() {
		Car clone = new Car(name);
		clone.maxSpeed = maxSpeed;
		clone.length = length;
		clone.tags = new HashSet<String>(tags);
		clone.notes = notes;
		return clone;
	}

	private Button cloneButton() {
		return new Button(t("copy"),Map.of(REALM,REALM_CAR,ID,id(),ACTION,ACTION_ADD));
	}

	public static Car get(Object id) {		
		return isNull(id) ? null : cars.get(new Id(""+id)); // try to get by id
	}
	
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(LENGTH, length);
		if (maxSpeed != 0) json.put(MAX_SPEED, maxSpeed);
		json.put(STOCK_ID, stockId);
		if (!tags.isEmpty()) json.put(TAGS, tags);
		return json;
	}
	
	/**
	 * If arguments are given, the first is taken as content, the second as tag type.
	 * If no content is supplied, name is set as content.
	 * If no type is supplied, "span" is preset.
	 * @param args
	 * @return
	 */
	public Tag link(String...args) {
		String tx = args.length<1 ? name()+NBSP : args[0];
		String type = args.length<2 ? "span" : args[1];
		return link(type, tx);
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
		BufferedReader file = new BufferedReader(new FileReader(filename, UTF8));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			String name = json.getString(Car.NAME);
			Id id = Id.from(json);
			Car car = json.has(Locomotive.LOCOMOTIVE) ? new Locomotive(name, id) : new Car(name,id);
			car.load(json).plan(plan);
			
			line = file.readLine();
		}
		file.close();
	}
	
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		if (json.has(MAX_SPEED)) maxSpeed = json.getInt(MAX_SPEED);
		if (json.has(STOCK_ID)) stockId = json.getString(STOCK_ID);
		if (json.has(TAGS)) json.getJSONArray(TAGS).forEach(elem -> { tags.add(elem.toString()); });
		return this;
	}
	
	public static Object manager() {
		Window win = new Window("car-manager", t("Car manager"));
		new Tag("h4").content(t("known cars")).addTo(win);
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Length"),t("Tags"),t("Actions"));
		cars.values()
			.stream()
			.filter(car -> !(car instanceof Locomotive))
			.forEach(car -> table.addRow(
					car.stockId,
					car.link(),
					car.maxSpeed == 0 ? "–":(car.maxSpeed+NBSP+speedUnit),
					car.length+NBSP+lengthUnit,
					String.join(", ", car.tags()),
					car.cloneButton()
			));
		table.addTo(win);
		
		Form form = new Form("add-car-form");
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(REALM,REALM_CAR).hideIn(form);
		Fieldset fieldset = new Fieldset(t("add new car"));
		new Input(Locomotive.NAME, t("new car")).addTo(new Label(t("Name:")+NBSP)).addTo(fieldset);
		new Button(t("Apply"),form).addTo(fieldset);
		fieldset.addTo(form).addTo(win);
		return win;
	}

	public int maxSpeed() {
		return maxSpeed;
	}
	
	String name(){
		return name;
	}
	
	public Plan plan() {
		return plan;
	}
	
	public Car plan(Plan plan) {
		this.plan = plan;
		return this;
	}
	
	public Window properties() {
		
		List<Tag> formInputs = List.of(
			new Input(NAME,name).addTo(new Label(t("Name")+NBSP)),
			new Input(STOCK_ID,stockId).addTo(new Label(t("Stock ID")+NBSP)),
			new Input(LENGTH,length).attr("type", "number").addTo(new Label(t("Length")+NBSP)).content(NBSP+lengthUnit),
			new Input(TAGS,String.join(", ", tags)).addTo(new Label(t("Tags")+NBSP)),
			new Input(MAX_SPEED, maxSpeed).numeric().addTo(new Label(t("Maximum speed")+":"+NBSP)).content(NBSP+speedUnit)
		);
		
		Fieldset fieldset = new Fieldset(t("Train"));
		if (train != null) train.link().addTo(fieldset);
		
		return super.properties(List.of(),formInputs,List.of(fieldset));
	}

	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<Id, Car> entry: cars.entrySet()) file.write(entry.getValue().json()+"\n");
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
	
	public Train train() {
		return train;
	}

	public void train(Train train) {
		this.train = train;
	}

	protected Car update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(NAME)) name = params.get(NAME).trim();
		if (params.containsKey(LENGTH)) length = Integer.parseInt(params.get(LENGTH));
		if (params.containsKey(MAX_SPEED)) maxSpeed  = Integer.parseInt(params.get(MAX_SPEED));
		if (params.containsKey(STOCK_ID)) stockId  = params.get(STOCK_ID);
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

	@Override
	public int compareTo(Car o) {
		return (stockId+":"+name).compareTo(o.stockId+":"+o.name);
	}
}
