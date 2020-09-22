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

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Window;

public class Car {
	public static final String ID = "id";
	public static final String NAME = "name";
	private static final String LENGTH = "length";
	private static final String SHOW = "show";
	static HashMap<String,Car> cars = new HashMap<String, Car>();
	public int length;
	private String name;
	private String id;
	private Train train;
	
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
		return json;
	}
	
	public Tag link(String tagClass) {
		return new Tag(tagClass).clazz("link").attr("onclick","car("+id+",'"+Car.SHOW+"')").content(name());
	}
	
	public static void loadAll(String filename) throws IOException {
		cars.clear();
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			String name = json.getString(Car.NAME);
			String id = json.getString(Car.ID);
			Car car = json.has(Locomotive.LOCOMOTIVE) ? new Locomotive(name, id) : new Car(name,id);
			car.load(json);
			
			line = file.readLine();
		}
		file.close();
	}
	
	protected void load(JSONObject json) {
		if (json.has(ID)) id = json.getString(ID);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
	}
	
	String name(){
		return name;
	}
	
	public Object properties() {
		Window win = new Window("car-props", t("Properties of {}",this));
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
}
