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
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

/**
 * Base Class for all other actions
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Action extends BaseClass {
	private static final HashMap<Id,Action> actions = new HashMap<Id, Action>();
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	private static final String PREFIX = Action.class.getPackageName();
	
	public Action(BaseClass parent) {
		actions.put(id(), this);
		parent(parent);
	}
	
	public BaseClass context() {
		BaseClass context = this;
		while (context instanceof Action && isSet(context.parent())) context = context.parent();
		return context;
	}
	
	public static Action create(String type,BaseClass parent) {
		try {
			return (Action) Class.forName(PREFIX+"."+type).getDeclaredConstructor(BaseClass.class).newInstance(parent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean equals(Action other) {
		return this.toString().equals(other.toString());
	}

	public abstract boolean fire(Context context);
	
	public static Action get(Id actionId) {
		return actions.get(actionId);
	}

	public JSONObject json() {
		return new JSONObject().put(TYPE, getClass().getSimpleName());
	}
	
	public static List<Class<? extends Action>> classes() {
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
	
	public boolean moveUp() {
		BaseClass parent = parent();
		if (parent instanceof ActionList) {
			ActionList actionList = (ActionList) parent;
			return actionList.moveUp(this);
		}
		LOG.error("Action.drop() called on Action ({}) whose parent ({}) is not an ActionList!",this,parent); 
		return false;
	}

/*	@Override
	public Window properties() { // goes up to first ancestor, which is not an Action
		return parent().properties();
	}*/
	
	public static Tag selector() {
		Select select = new Select(TYPE);
		TreeMap<String, String> names = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Class<? extends Action> clazz : Action.classes()) {
			String s = t(clazz.getSimpleName());
			names.put(s, clazz.getSimpleName());
		}
		
		for (Entry<String, String> entry : names.entrySet()) select.addOption(entry.getValue(), entry.getKey());
		return select.addTo(new Label(t("Action type:")+NBSP));
	}
	
	protected static String t(String tex,Object...fills) {
		return Translation.get(Application.class, tex, fills);
	}
	
	@Override
	public String toString() {
		return t(getClass().getSimpleName());
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		super.update(params);
		return context().properties();		
	}
}
