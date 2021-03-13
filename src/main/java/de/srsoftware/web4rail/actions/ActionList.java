package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.Iterator;
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
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class ActionList extends Action implements Iterable<Action>{
	static final Logger LOG = LoggerFactory.getLogger(ActionList.class);
	private static final String ACTIONS = "actions";

	protected Vector<Action> actions;
	
	public ActionList(BaseClass parent) {
		super(parent);
		actions = new Vector<Action>();
	}

	private Tag actionTypeForm() {
		Window win = new Window("add-action-form", t("Add action to action list"));		
		Form typeForm = new Form("add-action-to-"+id);
		new Input(REALM, REALM_ACTIONS).hideIn(typeForm);
		new Input(ID,id).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD).hideIn(typeForm);
		Action.selector().addTo(typeForm);
		return new Button(t("Create action"),typeForm).addTo(typeForm).addTo(win);
	}
	
	public ActionList add(Action action) {
		action.parent(this);
		actions.add(action);		
		return this;
	}
	
	private Object addActionForm(HashMap<String, String> params, Plan plan) {
		String type = params.get(TYPE);
		if (isNull(type)) return actionTypeForm();
		Action action = Action.create(type,this);
		if (action instanceof Action) {
			add(action);
			return context().properties();
		} 
		return new Tag("span").content(t("Unknown action type: {}",type)).addTo(actionTypeForm());
	}
	
	public void clear() {
		while (!actions.isEmpty()) actions.firstElement().remove();
	}
	
	@Override
	public boolean correspondsTo(Action other) {
		if (other instanceof ActionList) {
			ActionList otherAL = (ActionList) other;
			if (actions.size() != otherAL.actions.size()) return false;
			for (int i=0; i<actions.size(); i++) {
				if (!actions.get(i).correspondsTo(otherAL.actions.get(i))) return false;
			}
			return true;
		}
		return false;
	}


	public boolean drop(Action action) {
		return actions.remove(action);
	}

	public boolean fire(Context context,Object cause) {
		for (Action action : actions) {
			if (context.invalidated()) {
				LOG.debug("Context has been invalidated, aborting {}",this);
				return false;
			}
			LOG.debug("firing \"{}\"",action);
			if (!action.fire(context,cause)) {
				LOG.warn("{} failed",action);
				return false;			
			}
		}
		return true;
	}
	
	public Integer getSpeed(Context context) {
		LOG.debug("{}.getSpeed({})",this,context);
		Integer speed = null;
		for (Action action : this) {
			if (action instanceof SetSpeed) speed = ((SetSpeed)action).getSpeed();
			if (isNull(speed) && action instanceof ActionList) {
				Integer listSpeed = ((ActionList)action).getSpeed(context);
				if (isSet(listSpeed)) speed = listSpeed;
			}
		}
		return speed;
	}

	@Override
	public Iterator<Action> iterator() {
		return actions.iterator();
	}
			
	public boolean isEmpty() {
		return actions.isEmpty();
	}

	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONArray jActions = new JSONArray();
		actions.forEach(action -> jActions.put(action.json()));
		json.put(ACTIONS,jActions);
		return json;
	}

	public Tag list() {
		Tag span = new Tag("span");
		button(t("add action"), Map.of(ACTION, ACTION_ADD)).addTo(span);
		button(t("export"), Map.of(ACTION, ACTION_SAVE)).addTo(span);
		if (!isEmpty()) {
			Tag list = new Tag("ol");
			for (Action action : actions) {
				Tag item = action.link("span",action).addTo(new Tag("li")).content(NBSP);
				action.button("-", Map.of(ACTION,ACTION_DROP)).addTo(item);
				action.button("↑", Map.of(ACTION,ACTION_MOVE)).addTo(item);
				if (action instanceof ActionList) ((ActionList) action).list().addTo(item);
				item.addTo(list);
			}
			list.addTo(span);
		}
				
		return span;
	}

	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(ACTIONS)) {
			JSONArray list = json.getJSONArray(ACTIONS);
			for (Object o : list) {
				if (o instanceof JSONObject) {
					JSONObject jsonObject = (JSONObject) o;
					String type = mapOldTypes(jsonObject.getString(TYPE));
					Action action = Action.create(type,this);
					if (isSet(action)) add(action.load(jsonObject));
				}
			}
		}
		return this;
	}
	
	private String mapOldTypes(String type) {
		switch (type) {
			case "AddDestination":
				return AddRemoveDestination.class.getSimpleName();
			default:
				return type;
		}		
	}

	public void merge(ActionList oldActions) {
		for (Action oldAction : oldActions.actions) {
			for (Action newAction : actions) {
				if (oldAction.correspondsTo(newAction)) {
					actions.remove(newAction);
					LOG.debug("new action {} replaced by {}",newAction,oldAction);
					break;
				}
			}
			add(oldAction);
		}
		oldActions.actions.clear();
	}
	
	public boolean moveUp(Action action) {
		if (isNull(action)) return false;
		if (actions.firstElement() == action && parent() instanceof ActionList) {
			ActionList parentList = (ActionList) parent();
			for (int i=0; i<parentList.actions.size(); i++) {
				if (parentList.actions.get(i) == this) {
					actions.remove(0);
					parentList.actions.insertElementAt(action, i);
					action.parent(parentList);
					return true;
				}
			}
		}
		for (int i=1; i<actions.size(); i++) {
			if (actions.elementAt(i) == action) {
				actions.remove(i);
				Action aboveAction = actions.get(i-1);
				if (aboveAction instanceof ActionList) {
					((ActionList)aboveAction).add(action);
				} else actions.insertElementAt(action, i-1);
				return true;
			}
		}
		return false;
	}
	
	public ActionList prepend(Action action) {
		action.parent(this);
		actions.insertElementAt(action, 0);		
		return this;
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
				if (isNull(action)) return t("No action with id {} found!",actionId);
				BaseClass context = action.context();
				action.remove();
				return context.properties();
			case ACTION_MOVE:
				if (isNull(action)) return t("No action with id {} found!",actionId);
				if (action.moveUp()) return action.context().properties();
				Window result = action.context().properties();
				return new Tag("fieldset").content(t("Was not able to move \"{}\" up!",action)).addTo(result); 
			case ACTION_PROPS:
				return action.properties();
			case ACTION_SAVE:
				return action.jsonImportExport(params);
			case ACTION_UPDATE:
				return action.update(params);
		}
		return t("Unknown action: {}",command);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Fieldset fieldset = new Fieldset(t("Actions"));
		list().addTo(fieldset);
		postForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public BaseClass remove() {
		LOG.debug("Removing Action List ({}) {}",id(),this);
		super.remove();
		while (!actions.isEmpty()) actions.lastElement().remove();
		return this;
	}
	
	@Override
	public void removeChild(BaseClass child) {
		actions.remove(child);
		super.removeChild(child);
	}
	
	@Override
	public String toString() {
		return actions.isEmpty() ? "[no actions]" : actions.toString();
	}
}
