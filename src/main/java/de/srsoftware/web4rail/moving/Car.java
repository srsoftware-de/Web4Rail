package de.srsoftware.web4rail.moving;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

public class Car {
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String LENGTH = "length";
	private static HashMap<String,Car> cars = new HashMap<String, Car>();
	public int length;
	private String name;
	private String id;
	
	public Car(String name) {
		this(name,null);
	}
	
	public Car(String name, String id) {
		this.name = name;
		this.id = id == null ? ""+new Date().getTime() : id;
		cars.put(this.id, this);
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

	String name(){
		return name;
	}

	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<String, Car> entry: cars.entrySet()) {
			file.write(entry.getValue().json()+"\n");
		}
		file.close();
	}
}
