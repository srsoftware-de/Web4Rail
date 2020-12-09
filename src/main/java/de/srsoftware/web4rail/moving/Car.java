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
	public static boolean FORWARD = true;
	public static boolean REVERSE = false;
	private static final String LENGTH = "length";
	private static final String STOCK_ID = "stock-id";
	private static final String TAGS = "tags";
	private static final String MAX_SPEED = "max_speed";
	private static final String MAX_SPEED_REVERSE = "max_speed_reverse";
	protected HashSet<String> tags = new HashSet<String>();
	
	private String name;
	public int length;
	protected String stockId = "";
	private Train train;
	protected int maxSpeedForward = 0;
	protected int maxSpeedReverse = 0;
	protected boolean orientation = FORWARD;
	
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
					car.clone();
				} else new Car(params.get(Car.NAME)).parent(plan);
				return Car.manager();
			case ACTION_PROPS:
				return car == null ? Car.manager() : car.properties();
			case ACTION_UPDATE:
				return car.update(params);
		}
		if (car instanceof Locomotive) return Locomotive.action(params,plan);
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	public Car clone() {
		Car clone = new Car(name);
		clone.maxSpeedForward = maxSpeedForward;
		clone.length = length;
		clone.tags = new HashSet<String>(tags);
		clone.notes = notes;
		clone.parent(parent());
		clone.register();
		return clone;
	}

	private Button cloneButton() {
		return new Button(t("copy"),Map.of(REALM,REALM_CAR,ID,id(),ACTION,ACTION_ADD));
	}
	
	@Override
	public int compareTo(Car o) {
		return (stockId+":"+name).compareTo(o.stockId+":"+o.name);
	}

	public static Car get(Object id) {		
		return isNull(id) ? null : cars.get(new Id(""+id)); // try to get by id
	}
	
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(LENGTH, length);
		if (maxSpeedForward != 0) json.put(MAX_SPEED, maxSpeedForward);
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
	
	public static void loadAll(String filename, Plan plan) throws IOException {
		cars.clear();
		BufferedReader file = new BufferedReader(new FileReader(filename, UTF8));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			String name = json.getString(Car.NAME);
			Id id = Id.from(json);
			Car car = json.has(Locomotive.LOCOMOTIVE) ? new Locomotive(name, id) : new Car(name,id);
			car.load(json).parent(plan);
			
			line = file.readLine();
		}
		file.close();
	}
	
	public Car load(JSONObject json) {
		super.load(json);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		if (json.has(MAX_SPEED)) {
			maxSpeedForward = json.getInt(MAX_SPEED);
			maxSpeedReverse = maxSpeedForward;
		}
		if (json.has(MAX_SPEED_REVERSE)) maxSpeedReverse = json.getInt(MAX_SPEED_REVERSE);
		if (json.has(STOCK_ID)) stockId = json.getString(STOCK_ID);
		if (json.has(TAGS)) json.getJSONArray(TAGS).forEach(elem -> { tags.add(elem.toString()); });
		return this;
	}
	
	public static Object manager() {
		Window win = new Window("car-manager", t("Car manager"));
		new Tag("h4").content(t("known cars")).addTo(win);
		new Tag("p").content(t("Click on a name to edit the entry.")).addTo(win);
		
		Table table = new Table().addHead(t("Stock ID"),t("Name"),t("Max. Speed",speedUnit),t("Length"),t("Train"),t("Tags"),t("Actions"));
		cars.values()
			.stream()
			.filter(car -> !(car instanceof Locomotive))
			.sorted((c1,c2)->{
				try {
					return Integer.parseInt(c1.stockId)-Integer.parseInt(c2.stockId);
				} catch (NumberFormatException nfe) {
					return c1.stockId.compareTo(c2.stockId);	
				}
				
			})
			.forEach(car -> table.addRow(
					car.stockId,
					car.link(),
					car.maxSpeedForward == 0 ? "–":(car.maxSpeedForward+NBSP+speedUnit),
					car.length+NBSP+lengthUnit,
					isSet(car.train) ? car.train.link("span", car.train) : "",
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
		return maxSpeedForward;
	}
	
	String name(){
		return name;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Name"),new Input(NAME,name));
		formInputs.add(t("Stock ID"),new Input(STOCK_ID,stockId));
		formInputs.add(t("Length"),new Input(LENGTH,length).attr("type", "number").addTo(new Tag("span")).content(NBSP+lengthUnit));
		formInputs.add(t("Tags"), new Input(TAGS,String.join(", ", tags)));
		Tag div = new Tag("div");
		new Input(MAX_SPEED,         maxSpeedForward).numeric().addTo(new Tag("p")).content(NBSP+speedUnit+NBSP+t("forward")).addTo(div);
		new Input(MAX_SPEED_REVERSE, maxSpeedReverse).numeric().addTo(new Tag("p")).content(NBSP+speedUnit+NBSP+t("reverse")).addTo(div);
		formInputs.add(t("Maximum Speed"),div);
		
		Fieldset fieldset = new Fieldset(t("Train"));
		if (train != null) train.link().addTo(fieldset);
		
		postForm.add(fieldset);
		
		return super.properties(preForm,formInputs,postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == train) train = null;
		super.removeChild(child);
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

	protected Window update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(NAME)) name = params.get(NAME).trim();
		if (params.containsKey(LENGTH)) length = Integer.parseInt(params.get(LENGTH));
		if (params.containsKey(MAX_SPEED)) maxSpeedForward  = Integer.parseInt(params.get(MAX_SPEED));
		if (params.containsKey(MAX_SPEED_REVERSE)) maxSpeedReverse = Integer.parseInt(params.get(MAX_SPEED_REVERSE));
		if (params.containsKey(STOCK_ID)) stockId  = params.get(STOCK_ID);
		if (params.containsKey(TAGS)) {
			String[] parts = params.get(TAGS).replace(",", " ").split(" ");
			tags.clear();
			for (String tag : parts) {
				tag = tag.trim();
				if (!tag.isEmpty()) tags.add(tag);
			}
		}
		return properties();
	}

	public String turn() {
		orientation = !orientation;
		return t("Reversed {}.",this);
	}
}
