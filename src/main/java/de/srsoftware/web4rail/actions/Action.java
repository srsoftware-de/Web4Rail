package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;

/**
 * Base Class for all other actions
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Action extends BaseClass {
	private static final HashMap<Integer,Action> actions = new HashMap<Integer, Action>();
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	private static final String PREFIX = Action.class.getPackageName();
	protected int id;
	
	public static class Context {
		public Contact contact = null;
		public Route route = null;
		public Train train = null;
		public Block block = null;
		public Direction direction = null;
		
		public Context(Contact c, Route r, Train t, Block b, Direction d) {
			contact = c;
			route = r;
			train = t;
			block = b;
			direction = d;
		}
		
		public Context(Contact c) {			
			contact = c;
			setRoute(contact.route());
		}
		
		public Context(Train train) {
			setTrain(train);
		}
		
		public Context(Route route) {
			setRoute(route);
		}
		
		protected Context clone() {
			return new Context(contact, route, train, block, direction);
		}

		private void setRoute(Route route) {
			this.route = route;
			if (isSet(route)) setTrain(route.train);
			
		}

		private void setTrain(Train train) {
			this.train = train;
			if (isSet(train)) {
				if (isNull(route)) route = train.route;
				setBlock(train.currentBlock());
				setDirection(train.direction());
			}			
		}

		private void setDirection(Direction dir) {
			direction = dir;
		}

		private void setBlock(Block block) {
			this.block = block;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer(getClass().getSimpleName());
			sb.append("(");
			sb.append(t("Train: {}",train));
			if (isSet(route))   sb.append(", "+t("Route: {}",route));
			if (isSet(contact)) sb.append(", "+t("Contact: {}",contact));
			sb.append(")");
			return sb.toString();
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
	
	public boolean equals(Action other) {
		return this.toString().equals(other.toString());
	}

	public abstract boolean fire(Context context);
	
	public static Action get(int actionId) {
		return actions.get(actionId);
	}
	
	public int id() {
		return id;
	}

	public JSONObject json() {
		return new JSONObject().put(TYPE, getClass().getSimpleName());
	}
	
	public static List<Class<? extends Action>> list() {
		return List.of(
			BrakeStart.class,
			BrakeStop.class,
			BrakeCancel.class,
			ConditionalAction.class,
			DelayedAction.class,
			DetermineTrainInBlock.class,
			FinishRoute.class,
			PreserveRoute.class,
			SendCommand.class,
			SetContextTrain.class,
			SetDisplayText.class,
			SetPower.class,
			SetRelay.class,
			SetSignal.class,
			SetSpeed.class,
			ShowText.class,
			StopAllTrains.class,
			StopAuto.class,
			TriggerContact.class,
			TurnTrain.class
		);
	}
	
	public Action load(JSONObject json) {
		return this;
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

	public Window properties(HashMap<String, String> params) {
		return new Window("action-props-"+id, t("Properties of {}",this.getClass().getSimpleName()));
	}
	
	protected static String t(String tex,Object...fills) {
		return Translation.get(Application.class, tex, fills);
	}
	
	@Override
	public String toString() {
		return t(getClass().getSimpleName());
	}

	protected Object update(HashMap<String, String> params) {
		return t("Nothing changed");
	}
}
