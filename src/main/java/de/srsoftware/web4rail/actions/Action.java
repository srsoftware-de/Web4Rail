package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Contact;

public abstract class Action {
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	private static final String TYPE = "type";

	
	public static class Context {
		public Plan plan = null;
		public Contact contact = null;
		public Route route = null;
		public Train train = null;
		
		public Context(Contact c) {
			contact = c;
			route = contact.route();
			if (route == null) return;
			train = route.train;
		}
	}

	public abstract void fire(Context context) throws IOException;

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
			case "TurnTrain":
				return new TurnTrain(json.getInt(RouteAction.ROUTE));
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
