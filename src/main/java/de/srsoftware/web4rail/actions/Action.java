package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.json.JSONObject;

import de.keawe.tools.translations.Translation;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;

public abstract class Action {
	private static final String TYPE = "type";

	public abstract void fire(Plan plan) throws IOException;
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(TYPE, getClass().getSimpleName());
		return json;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public static Action load(JSONObject json) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		String clazz = json.getString(TYPE);
		switch (clazz) {
			case "ActivateRoute":
				return new ActivateRoute(json.getInt(RouteAction.ROUTE));
			case "FinishRoute":
				return new FinishRoute(json.getInt(RouteAction.ROUTE));
			case "SetSignalsToStop":
				return new SetSignalsToStop(json.getInt(RouteAction.ROUTE));
			case "SpeedReduction":
				return new SpeedReduction(json.getInt(RouteAction.ROUTE), json.getInt(SpeedReduction.MAX_SPEED));
		}
		return null;
	}
	
	public static Window propForm(HashMap<String, String> params) {
		return new Window("action-props", "Action properties");
	}
	
	protected static String t(String tex,Object...fills) {
		return Translation.get(Application.class, tex, fills);
	}
}
