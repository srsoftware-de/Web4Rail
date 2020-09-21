package de.srsoftware.web4rail.actions;

import java.io.IOException;

import org.json.JSONObject;

public abstract class Action {
	private static final String TYPE = "type";

	public abstract void fire() throws IOException;
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(TYPE, getClass().getSimpleName());
		return json;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
