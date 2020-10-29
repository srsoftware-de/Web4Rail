package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Contact;

public abstract class Action implements Constants {
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	protected int id;
	
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
	
	public Action() {
		id = Application.createId();
	}

	public abstract boolean fire(Context context) throws IOException;
	
	public int id() {
		return id;
	}

	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(TYPE, getClass().getSimpleName());
		return json;
	}
	
	protected Tag link(int actionId, String context) {
		Map<String, String> props = Map.of(REALM,REALM_ACTIONS,ID,actionId+"/"+id,ACTION,ACTION_PROPS,CONTEXT,context);
		String action = "request("+(new JSONObject(props).toString().replace("\"", "'"))+")";
		return new Tag("span").content(toString()).attr("onclick", action);
	}
	
	public static Action load(JSONObject json) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		String clazz = json.getString(TYPE);
		switch (clazz) {
			case "ActivateRoute":
				return new ActivateRoute();
			case "FinishRoute":
				return new FinishRoute();
			case "SetSignalsToStop":
				return new SetSignalsToStop();
			case "SpeedReduction":
				return new SpeedReduction(json.getInt(SpeedReduction.MAX_SPEED));
			case "TurnTrain":
				return new TurnTrain();
		}
		return null;
	}
	
	public Window properties(HashMap<String, String> params) {
		return new Window("action-props-"+id, t("Properties of {}",this.getClass().getSimpleName()));
	}
	
	protected static String t(String tex,Object...fills) {
		return Translation.get(Application.class, tex, fills);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	protected Object update(HashMap<String, String> params) {
		return t("Nothing changed");
	}
}