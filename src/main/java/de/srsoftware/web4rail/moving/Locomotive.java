package de.srsoftware.web4rail.moving;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;

public class Locomotive extends Car {
	
	private static final String REVERSE = "reverse";
	public static final String LOCOMOTIVE = "locomotive";
	private boolean reverse = false;

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
		json.put(LOCOMOTIVE, loco);
		return json;
	}
	
	@Override
	protected void load(JSONObject json) {
		super.load(json);
		if (json.has(REVERSE)) reverse = json.getBoolean(REVERSE);
	}

	public void setSpeed(int v) {
		// TODO Auto-generated method stub
		
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
		return win;
	}
}
