package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass.Context;
import de.srsoftware.web4rail.BaseClass.Id;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;

public class ActionList extends Vector<Action> implements Constants{
	private static final Logger LOG = LoggerFactory.getLogger(ActionList.class);
	
	private static final long serialVersionUID = 4862000041987682112L;
	private static final HashMap<Id, ActionList> actionLists = new HashMap<Id, ActionList>();
	private Id id;
	
	public ActionList() {
		id = new Id();
		actionLists.put(id,this);
	}
	
	private static Id actionId(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return null;
		String[] parts = params.get(ID).split("/");
		if (parts.length<2) return null;
		return new Id(parts[1]);
	}

	private static Id actionListId(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return null;
		String[] parts = params.get(ID).split("/");
		return new Id(parts[0]);
	}
	
	private Object actionTypeForm(Window win, String context) {
		Form typeForm = new Form("add-action-to-"+id);
		new Input(REALM, REALM_ACTIONS).hideIn(typeForm);
		new Input(ID,id).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD).hideIn(typeForm);
		new Input(CONTEXT,context).hideIn(typeForm);
		Action.selector().addTo(typeForm);
		return new Button(t("Create action"),typeForm).addTo(typeForm).addTo(win);
	}
	
	private Object addActionForm(HashMap<String, String> params, Plan plan) {
		Window win = new Window("add-action-form", t("Add action to action list"));		
		String type = params.get(TYPE);
		String context = params.get(CONTEXT);
		if (type == null) return actionTypeForm(win,context);
		
		Action action = Action.create(type);
		if (action instanceof Action) {
			add(action);
			return plan.showContext(params);
		} 
		actionTypeForm(win,context);
		new Tag("span").content(t("Unknown action type: {}",type)).addTo(win);
		return win;			
	}
	
	public void addActionsFrom(ActionList other) {
		for (Action otherAction : other) {
			//LOG.debug("old action ({}): {}",otherAction.getClass().getSimpleName(),otherAction);
			boolean exists = false;
			int len = this.size();
			for (int i=0; i<len; i++) {
				Action thisAction = this.get(i);
				LOG.debug("→ {} ?",thisAction);
				if (thisAction.equals(otherAction)) {
					LOG.debug("Action already existing!");
					exists = true;
					break;
				}
			}
			if (exists) {
				LOG.debug("action not added.");
			} else {
				this.add(otherAction);
				LOG.debug("action added.");
			}
		}
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
				Tag act = action.link("span", action+NBSP, Map.of(CONTEXT,context,ID,id+"/"+action.id())).addTo(new Tag("li")); 
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
				if (action instanceof DelayedAction) {
					DelayedAction da = (DelayedAction) action;
					da.children().addTo(act, context);
				}
				act.addTo(ul);
				first = false;
			}
			ul.addTo(link);
		}		
	}
	
	public boolean drop(Id actionId) {
		for (Action action : this) {
			if (action.id().equals(actionId)) {
				this.remove(action);
				return true;				
			}
		}
		return false;
	}
	
	public boolean fire(Context context) {
		if (!isEmpty())	LOG.debug(t("Firing {}"),this);
		for (Action action : this) {
			if (!action.fire(context)) return false;			
		}
		return true;
	}

	public Id id() {
		return id;
	}
	
	public JSONArray json() {
		JSONArray result = new JSONArray();
		for (Action action : this) result.put(action.json());
		return result;
	}

	public static ActionList load(JSONArray list) {
		ActionList actionList = new ActionList();
		for (Object o : list) {
			if (o instanceof JSONObject) {
				JSONObject json = (JSONObject) o;
				Action action = Action.create(json.getString(TYPE));
				if (action != null) actionList.add(action.load(json));
			}
		}
		return actionList;
	}
	
	public boolean moveUp(Id actionId) {
		for (int i=1; i<size(); i++) {
			if (actionId.equals(elementAt(i).id())) {
				Action action = remove(i);
				insertElementAt(action, i-1);
				return true;
			}
		}
		return false;
	}
	
	public static Object process(HashMap<String, String> params, Plan plan) {
		Id listId = actionListId(params);
		if (listId == null) return t("No action list id passed to ActionList.process()!");
		ActionList actionList = actionLists.get(listId);

		Id actionId = actionId(params);
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to ActionList.process()!");
		if (actionList == null && !List.of(ACTION_UPDATE,ACTION_PROPS).contains(action)) return t("No action list with id {} found!",listId);
		
		switch (action) {
			case ACTION_ADD:
				return actionList.addActionForm(params,plan);
			case ACTION_DROP:
				return actionList.drop(actionId) ? plan.showContext(params) : t("No action with id {} found!",actionId);
			case ACTION_MOVE:
				return actionList.moveUp(actionId) ? plan.showContext(params) : t("No action with id {} found!",actionId);
			case ACTION_PROPS:
				return propsOf(params);
			case ACTION_UPDATE:
				return update(actionId,params,plan);
		}
		return t("Unknown action: {}",action);
	}
	
	private static Object propsOf(HashMap<String, String> params) {
		Id actionId = actionId(params); 
		Action action = Action.get(actionId);
		if (action != null) return action.properties(params);
		return t("No action with id {} found!",actionId);
	}
	
	private static String t(String text,Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
	
	private static Object update(Id actionId, HashMap<String, String> params, Plan plan) {
		Action action = Action.get(actionId);
		if (action != null) {
			plan.stream(action.update(params).toString());
			return plan.showContext(params);
		}
		return t("No action with id {} found.",actionId);
	}
}
