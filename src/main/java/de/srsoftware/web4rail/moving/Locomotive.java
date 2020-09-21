package de.srsoftware.web4rail.moving;

import org.json.JSONObject;

public class Locomotive extends Car {
	
	private static final String REVERSE = "reverse";
	private static final String LOCOMOTIVE = "locomotive";
	private boolean reverse = false;

	public Locomotive(String name) {
		super(name);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONObject loco = new JSONObject();
		loco.put(REVERSE, reverse);
		json.put(LOCOMOTIVE, loco);
		return json;
	}

	public void setSpeed(int v) {
		// TODO Auto-generated method stub
		
	}
}
