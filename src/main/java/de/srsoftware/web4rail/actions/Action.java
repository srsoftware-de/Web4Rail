package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.TextArea;
import de.srsoftware.web4rail.tags.Window;

/**
 * Base Class for all other actions
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Action extends BaseClass {
	private static final HashMap<Id,Action> actions = new HashMap<Id, Action>();
	public static final Logger LOG = LoggerFactory.getLogger(Action.class);
	private static final String PREFIX = Action.class.getPackageName();
	private static final String JSON = "json";
	
	public Action(BaseClass parent) {
		actions.put(id(), this);
		parent(parent);
	}
	
	public static List<Class<? extends Action>> classes() {
		return List.of(
			AddRemoveDestination.class,
			AddRemoveTag.class,
			AlterDirection.class,
			BrakeStart.class,
			ConditionalAction.class,
			CoupleTrain.class,
			DelayedAction.class,
			DetermineTrainInBlock.class,
			DisableEnableBlock.class,
			EngageDecoupler.class,
			FinishRoute.class,
			Loop.class,
			PreserveRoute.class,
			ReactivateContact.class,
			SavePlan.class,
			SendCommand.class,
			SetContextTrain.class,
			SetDisplayText.class,
			SetPower.class,
			SetRelayOrSwitch.class,
			SetSignal.class,
			SetSpeed.class,
			SetTurnout.class,
			ShowText.class,
			SplitTrain.class,
			StartStopAuto.class,
			StopTrain.class,
			SwitchFunction.class,
			TriggerContact.class,
			WaitForContact.class
		);
	}

	public BaseClass context() {
		BaseClass context = this;
		while (context instanceof Action && isSet(context.parent())) context = context.parent();
		return context;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Action> T create(String type,BaseClass parent) {
		try {
			return (T) Class.forName(PREFIX+"."+type).getDeclaredConstructor(BaseClass.class).newInstance(parent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean correspondsTo(Action other) {
		return this.toString().equals(other.toString());
	}

	public abstract boolean fire(Context context,Object cause);
	
	public static Action get(Id actionId) {
		return actions.get(actionId);
	}

	public JSONObject json() {
		return new JSONObject().put(TYPE, getClass().getSimpleName());
	}
	
	protected Object jsonImportExport(HashMap<String, String> params) {
		if (params.containsKey(JSON)) {
			String jString = params.get(JSON);
			JSONObject json = new JSONObject(jString);
			if (this instanceof ActionList) {
				((ActionList)this).clear();
			}
			load(json);
			LoadCallback.fire();
			return context().properties();
		}
		Window win = new Window("json-import-export-"+id(), t("JSON code of {}",this));
		Form form = new Form("json-form-"+id());
		new Input(REALM, REALM_ACTIONS).hideIn(form);
		new Input(ID, id()).hideIn(form);
		new Input(ACTION, ACTION_SAVE).hideIn(form);
		new TextArea(JSON).clazz("json").content(json().toString(4)).addTo(form).addTo(win);
		new Button(t("update"),form).addTo(form);
		return win;
	}
		
	public Action load(JSONObject json) {
		super.load(json);
		return this;
	}
	
	public boolean moveUp() {
		BaseClass parent = parent();
		if (parent instanceof ActionList) {
			ActionList actionList = (ActionList) parent;
			return actionList.moveUp(this);
		}
		LOG.error("Action.moveUp() called on Action ({}) whose parent ({}) is not an ActionList!",this,parent); 
		return false;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Edit json"),button(t("export"), Map.of(ACTION, ACTION_SAVE)));

		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	public static Tag selector() {
		Select select = new Select(TYPE);
		TreeMap<String, String> names = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Class<? extends Action> clazz : Action.classes()) {
			String s = t(clazz.getSimpleName());
			names.put(s, clazz.getSimpleName());
		}
		
		for (Entry<String, String> entry : names.entrySet()) select.addOption(entry.getValue(), entry.getKey());
		return select.addTo(new Label(t("Action type")+COL));
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
