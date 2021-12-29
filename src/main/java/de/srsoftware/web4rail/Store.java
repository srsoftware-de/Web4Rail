package de.srsoftware.web4rail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Select;

public class Store {
	
	public interface Listener{
		public void storeUpdated();
	}
	
	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Store.class);
	
	private static HashMap<String,Store> stores = new HashMap<>();
	private HashSet<Listener> listeners = new HashSet<>();
	private String name;
	private String value = null;
	private Integer intValue = null;

	public Store(String name) {
		this.name = name;
		stores.put(name, this);
	}
	
	public Store addListener(Listener listener) {
		listeners.add(listener);
		return this;
	}

	public static Store get(String name) {
		Store store = stores.get(name);
		if (BaseClass.isNull(store)) store = new Store(name); 
		return store;
	}
	
	public Integer intValue() {
		return intValue;
	}

	public String name() {
		return name;
	}
	
	public static Set<String> names() {
		return stores.keySet();
	}
	
	public static void removeListener(Listener listener) {
		stores.values().forEach(store -> store.listeners.remove(listener));
	}
	
	public static Select selector(Store store) {
		Select selector = new Select(Store.class.getSimpleName());
		selector.addOption("",t("unset"));
		stores.keySet().forEach(name -> {
			Tag option = selector.addOption(name);
			if (BaseClass.isSet(store) && name.equals(store.name)) option.attr("selected", "selected");
		});
		return selector;
	}

	private static String t(String txt, Object...fills) {
		return BaseClass.t(txt, fills);
	}

	public void setValue(String newVal) {
		LOG.debug("Updating {}: {} → {}",name,value,newVal);
		value = newVal;
		intValue = null;
		for (char c : value.toCharArray()) {
			if (!Character.isDigit(c)) {
				if (BaseClass.isSet(intValue)) break;
			} else {
				int add = ((byte)c-48);
				intValue = BaseClass.isNull(intValue) ? add : 10*intValue + add;
			}  
		}
		listeners.forEach(listener -> listener.storeUpdated());
	}
	
	@Override
	public String toString() {
		return name+"&nbsp;("+(BaseClass.isNull(value) ? t("no value") : value)+")";
	}

	public String value() {
		return value;
	}
}
