package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public class ActionList extends Vector<Action> implements Constants{
	
	private static final long serialVersionUID = 4862000041987682112L;
	private static final Logger LOG = LoggerFactory.getLogger(ActionList.class);
	private int id;
	private static final HashMap<Integer, ActionList> actionLists = new HashMap<Integer, ActionList>();
	
	public ActionList() {
		id = Application.createId();
		actionLists.put(id,this);
	}
	
	private static Integer actionId(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return null;
		String[] parts = params.get(ID).split("/");
		if (parts.length<2) return null;
		return Integer.parseInt(parts[1]);
	}

	private static Integer actionListId(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return null;
		String[] parts = params.get(ID).split("/");
		return Integer.parseInt(parts[0]);
	}
	
	private Object actionTypeForm(Window win, String context) {
		String formId ="add-action-to-"+id;
		Tag typeForm = new Form(formId);
		new Input(REALM, REALM_ACTIONS).hideIn(typeForm);
		new Input(ID,id).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD).hideIn(typeForm);
		new Input(CONTEXT,context).hideIn(typeForm);
		Select select = new Select(TYPE);
		List<Class<? extends Action>> classes = List.of(
				ConditionalAction.class,
				SetSpeed.class,
				SetSignalsToStop.class,
				FinishRoute.class,
				TurnTrain.class,
				StopAuto.class,
				PowerOff.class
				);
		for (Class<? extends Action> clazz : classes) select.addOption(clazz.getSimpleName());
		select.addTo(new Label("Action type:")).addTo(typeForm);
		return new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(typeForm).addTo(win);
	}
	
	private Object addActionForm(HashMap<String, String> params, Plan plan) {
		Window win = new Window("add-action-form", t("Add action to action list"));		
		String type = params.get(TYPE);
		String context = params.get(CONTEXT);
		if (type == null) return actionTypeForm(win,context);
		
		switch (type) {
			case "ConditionalAction":
				add(new ConditionalAction());
				break;
			case "FinishRoute":
				add(new FinishRoute());
				break;
			case "SetSignalsToStop":
				add(new SetSignalsToStop());
				break;
			case "SetSpeed":
				add(new SetSpeed(0));
				break;
			case "TurnTrain":
				add(new TurnTrain());
				break;	
			case "StopAuto":
				add(new StopAuto());
				break;
			case "PowerOff":
				add(new PowerOff());
				break;
			default:
				actionTypeForm(win,context);
				new Tag("span").content(t("Unknown action type: {}",type)).addTo(win);
				return win;			
		}
		return plan.showContext(params);
	}
	
	public void addTo(Tag link, String context) {
		Map<String, Object> props = new HashMap<String, Object>(Map.of(
				REALM,REALM_ACTIONS,
				ID,id,
				ACTION,ACTION_ADD,
				CONTEXT,context));
		new Button(t("add action"),props).addTo(link);

		props.put(ACTION,ACTION_PROPS);
		if (!isEmpty()) {
			Tag ul = new Tag("ol");
			boolean first = true;
			for (Action action : this) {
				props.put(ID, id+"/"+action.id());
				Tag act = action.link(id,context).addTo(new Tag("li"));
				if (!first) {
					props.put(ACTION, ACTION_MOVE);
					new Button("↑",props).addTo(act);
				}
				props.put(ACTION, ACTION_DROP);
				new Button("-",props).addTo(act);
				if (action instanceof ConditionalAction) {
					ConditionalAction ca = (ConditionalAction) action;
					ca.children().addTo(act, context);
				}
				act.addTo(ul);
				first = false;
			}
			ul.addTo(link);
		}		
	}
	
	public boolean drop(int actionId) {
		for (Action action : this) {
			if (action.id() == actionId) {
				this.remove(action);
				return true;				
			}
		}
		return false;
	}
	
	public void fire(Context context) {
		LOG.debug("Firing {}",this);

		for (Action action : this) {
			try {
				action.fire(context);
			} catch (IOException e) {
				LOG.warn("Action did not fire properly: {}",action,e);
			}
		}
	}
	
	private Action getAction(int actionId) {
		for (Action action : this) {
			if (action.id == actionId) return action;
		}
		return null;
	}

	public int id() {
		return id;
	}
	
	public JSONArray json() {
		JSONArray result = new JSONArray();
		for (Action action : this) result.put(action.json());
		return result;
	}
	
	public boolean moveUp(int actionId) {
		for (int i=1; i<size(); i++) {
			if (actionId == elementAt(i).id()) {
				Action action = remove(i);
				insertElementAt(action, i-1);
				return true;
			}
		}
		return false;
	}
	
	public static Object process(HashMap<String, String> params, Plan plan) {
		Integer listId = actionListId(params);
		if (listId == null) return t("No action list id passed to ActionList.process()!");
		ActionList actionList = actionLists.get(listId);
		if (actionList == null) return t("No action list with id {} found!",listId);

		Integer actionId = actionId(params);
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to ActionList.process()!");
		
		switch (action) {
			case ACTION_ADD:
				return actionList.addActionForm(params,plan);
			case ACTION_DROP:
				return actionList.drop(actionId) ? plan.showContext(params) : t("No action with id {} found!",actionId);
			case ACTION_MOVE:
				return actionList.moveUp(actionId) ? plan.showContext(params) : t("No action with id {} found!",actionId);
			case ACTION_PROPS:
				return actionList.propsOf(params);
			case ACTION_UPDATE:
				return actionList.update(actionId,params,plan);
		}
		return t("Unknown action: {}",action);
	}
	
	private Object propsOf(HashMap<String, String> params) {
		int actionId = actionId(params); 
		Action action = getAction(actionId);
		if (action != null) return action.properties(params);
		return t("No action with id {} found!",actionId);
	}
	
	private static String t(String text,Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
	
	private Object update(int actionId, HashMap<String, String> params, Plan plan) {
		Action action = getAction(actionId);
		if (action != null) {
			plan.stream(action.update(params).toString());
			return plan.showContext(params);
		}
		return t("No action with id {} found.",actionId);
	}
}
