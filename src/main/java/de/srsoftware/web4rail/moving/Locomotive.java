package de.srsoftware.web4rail.moving;

import org.json.JSONObject;

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
}
