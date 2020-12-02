package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;

public class ActionList extends BaseClass{
	private static final Logger LOG = LoggerFactory.getLogger(ActionList.class);
	
	private static final HashMap<Id, ActionList> actionLists = new HashMap<Id, ActionList>();
	private Vector<Action> actions;
	private Id id;
	
	public ActionList() {
		id = new Id();
		actions = new Vector<Action>();
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
	
	public ActionList add(Action action) {
		actions.add(action);
		return this;
	}
	
	private Object addActionForm(HashMap<String, String> params, Plan plan) {
		Window win = new Window("add-action-form", t("Add action to action list"));		
		String type = params.get(TYPE);
		String context = params.get(CONTEXT);
		if (type == null) return actionTypeForm(win,context);
		Context parent = new Context(this);
		Action action = Action.create(type,parent);
		if (action instanceof Action) {
			add(action);
			return plan.showContext(params);
		} 
		actionTypeForm(win,context);
		new Tag("span").content(t("Unknown action type: {}",type)).addTo(win);
		return win;			
	}
	
	public void addActionsFrom(ActionList other) {
		for (Action otherAction : other.actions) {
			//LOG.debug("old action ({}): {}",otherAction.getClass().getSimpleName(),otherAction);
			boolean exists = false;
			int len = actions.size();
			for (int i=0; i<len; i++) {
				Action thisAction = actions.get(i);
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
		
	public boolean isEmpty() {
		return actions.isEmpty();
	}
	
	public boolean drop(Id actionId) {
		for (Action action : actions) {
			if (action.id().equals(actionId)) {
				actions.remove(action);
				return true;				
			}
		}
		return false;
	}
	
	public boolean fire(Context context) {
		if (!isEmpty())	LOG.debug(t("Firing {}"),this);
		for (Action action : actions) {
			if (!action.fire(context)) return false;			
		}
		return true;
	}

	public Id id() {
		return id;
	}
	
	public JSONArray jsonArray() {
		JSONArray result = new JSONArray();
		for (Action action : actions) result.put(action.json());
		return result;
	}

	public Fieldset list() {
		Fieldset fieldset = new Fieldset(t("Actions"));
		
		Map<String, Object> props = new HashMap<String, Object>(Map.of(
				REALM,REALM_ACTIONS,
				ID,id,
				ACTION,ACTION_PROPS));
		
		if (!isEmpty()) {
			Tag ul = new Tag("ol");
			boolean first = true;
			for (Action action : actions) {
				props.put(ID, id+"/"+action.id());
				Tag act = action.link("span", action+NBSP, Map.of(ID,id+"/"+action.id())).addTo(new Tag("li")); 
				if (!first) {
					props.put(ACTION, ACTION_MOVE);
					new Button("↑",props).addTo(act);
				}
				props.put(ACTION, ACTION_DROP);
				new Button("-",props).addTo(act);
// TODO: add children for conditionalActions and delayedActions
				act.addTo(ul);
				first = false;
			}
			ul.addTo(fieldset);
		}		
	
		return fieldset;
	}

	public ActionList load(JSONArray list) {
		Context parent = new Context(this);
		for (Object o : list) {
			if (o instanceof JSONObject) {
				JSONObject json = (JSONObject) o;
				Action action = Action.create(json.getString(TYPE),parent);
				if (action != null) add(action.load(json));
			}
		}
		return this;
	}
	
	public boolean moveUp(Id actionId) {
		for (int i=1; i<actions.size(); i++) {
			if (actionId.equals(actions.elementAt(i).id())) {
				Action action = actions.remove(i);
				actions.insertElementAt(action, i-1);
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
			case ACTION_UPDATE:
				return update(actionId,params,plan);
		}
		return t("Unknown action: {}",action);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Fieldset fieldset = new Fieldset(t("Actions"));
		list().addTo(fieldset);
		preForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm);
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
