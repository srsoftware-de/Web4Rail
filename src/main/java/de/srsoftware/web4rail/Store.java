package de.srsoftware.web4rail;

import java.util.HashMap;

public class Store {
	
	private static HashMap<String,Store> stores = new HashMap<>();
	private String name;
	private Integer value = null;

	public Store(String name) {
		this.name = name;
		stores.put(name, this);
	}

	public static Store get(String name) {
		Store store = stores.get(name);
		if (BaseClass.isNull(store)) store = new Store(name); 
		return store;
	}

	public String name() {
		return name;
	}
	
	public Integer value() {
		return value;
	}

	public void setValue(int newVal) {
		value = newVal;
	}
	
	@Override
	public String toString() {
		return name+"("+(BaseClass.isNull(value) ? BaseClass.t("no value") : value)+")";
	}

}
