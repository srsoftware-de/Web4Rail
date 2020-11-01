package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Contact;

public abstract class Action implements Constants {
	private static final HashMap<Integer,Action> actions = new HashMap<Integer, Action>();
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	private static final String PREFIX = Action.class.getPackageName();
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

		public Context(Train train) {
			this.train = train;
		}
	}
	
	public Action() {
		id = Application.createId();
		actions.put(id, this);
	}
	
	public static Action create(String type) {
		try {
			return (Action) Class.forName(PREFIX+"."+type).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public abstract boolean fire(Context context) throws IOException;
	
	public static Action get(int actionId) {
		return actions.get(actionId);
	}
	
	public int id() {
		return id;
	}

	public JSONObject json() {
		return new JSONObject().put(TYPE, getClass().getSimpleName());
	}
	
	protected Tag link(Integer parentId, String context) {
		Map<String, String> props = Map.of(REALM,REALM_ACTIONS,ID,parentId+"/"+id,ACTION,ACTION_PROPS,CONTEXT,context);
		String action = "request("+(new JSONObject(props).toString().replace("\"", "'"))+")";
		return new Tag("span").content(toString()+NBSP).attr("onclick", action);
	}
	
	public static List<Class<? extends Action>> list() {
		return List.of(
			ConditionalAction.class,
			SetSpeed.class,
			SetSignalsToStop.class,
			FreeStartBlock.class,
			FinishRoute.class,
			TurnTrain.class,
			StopAuto.class,
			PowerOff.class,
			SetRelay.class,
			DelayedAction.class
		);
	}
	
	public Action load(JSONObject json) {
		return this;
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

	public static Tag selector() {
		Select select = new Select(TYPE);
		TreeMap<String, String> names = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Class<? extends Action> clazz : Action.list()) {
			String s = t(clazz.getSimpleName());
			names.put(s, clazz.getSimpleName());
		}
		
		for (Entry<String, String> entry : names.entrySet()) select.addOption(entry.getValue(), entry.getKey());
		return select.addTo(new Label(t("Action type:")+NBSP));
	}
}
