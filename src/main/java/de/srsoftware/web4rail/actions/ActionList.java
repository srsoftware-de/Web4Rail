package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
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

public class ActionList extends Action{
	private static final Logger LOG = LoggerFactory.getLogger(ActionList.class);
	
	private Vector<Action> actions;
	
	public ActionList(BaseClass parent) {
		super(parent);
		actions = new Vector<Action>();
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
		Action action = Action.create(type,this);
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
	
	public boolean drop(Action action) {
		return actions.remove(action);
	}
	
	public boolean fire(Context context) {
		if (!isEmpty())	LOG.debug(t("Firing {}"),this);
		for (Action action : actions) {
			if (!action.fire(context)) return false;			
		}
		return true;
	}
	
	public JSONArray jsonArray() {
		JSONArray result = new JSONArray();
		for (Action action : actions) result.put(action.json());
		return result;
	}

	public Tag list() {
		Button button = button(t("Add action"), contextAction(ACTION_ADD_ACTION));
		Tag span = new Tag("span");
		button.addTo(span);
		
		if (!isEmpty()) {
			Tag list = new Tag("ol");
			boolean first = true;
			for (Action action : actions) {
				Tag item = action.link("span",action).addTo(new Tag("li"));
				if (first) {
					first = false;
				} else action.button("↑", contextAction(ACTION_MOVE)).addTo(item.content(NBSP));
				item.addTo(list);
			}
			list.addTo(span);
		}
				
		return span;
	}

	public ActionList load(JSONArray list) {
		for (Object o : list) {
			if (o instanceof JSONObject) {
				JSONObject json = (JSONObject) o;
				Action action = Action.create(json.getString(TYPE),this);
				if (action != null) add(action.load(json));
			}
		}
		return this;
	}
	
	public boolean moveUp(Action action) {
		for (int i=1; i<actions.size(); i++) {
			if (actions.elementAt(i) == action) {
				actions.remove(i);
				actions.insertElementAt(action, i-1);
				return true;
			}
		}
		return false;
	}
	
	public static Object process(HashMap<String, String> params, Plan plan) {
		String command = params.get(ACTION);
		if (command == null) return t("No action passed to ActionList.process()!");
		
		Id actionId = Id.from(params);
		Action action = Action.get(actionId);
		if (isNull(action)) return t("Id ({}) does not belong to Action!",actionId);
		ActionList actionList = action instanceof ActionList ? (ActionList) action : null;
		
		switch (command) {
			case ACTION_ADD:				
				if (isNull(actionList)) return t("Id ({}) does not belong to ActionList!",actionId);
				return actionList.addActionForm(params,plan);
			case ACTION_DROP:
				return action.drop() ? action.parent().properties() : t("No action with id {} found!",actionId);
			case ACTION_MOVE:
				return action.moveUp() ? action.parent().properties() : t("No action with id {} found!",actionId);
			case ACTION_PROPS:
				return action.properties();
			case ACTION_UPDATE:
				return action.update(params);
		}
		return t("Unknown action: {}",command);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Fieldset fieldset = new Fieldset(t("Actions"));
		list().addTo(fieldset);
		preForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm);
	}
}
