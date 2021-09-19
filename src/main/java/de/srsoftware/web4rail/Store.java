package de.srsoftware.web4rail;

import java.util.HashMap;

import org.slf4j.Logger;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Select;

public class Store {
	
	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Store.class);
	
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

	public void setValue(int newVal) {
		LOG.debug("Updating {}: {} → {}",name,value,newVal);
		value = newVal;
	}
	
	@Override
	public String toString() {
		return name+"&nbsp;("+(BaseClass.isNull(value) ? t("no value") : value)+")";
	}

	public Integer value() {
		return value;
	}
}
