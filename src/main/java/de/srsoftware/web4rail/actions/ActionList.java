package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
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
		id = new Date().hashCode();
		actionLists.put(id,this);
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

	public boolean drop(int actionId) {
		for (Action action : this) {
			if (action.id() == actionId) {
				this.remove(action);
				return true;				
			}
		}
		return false;
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

	public void addTo(Tag link) {
		Map<String, Object> props = new HashMap<String, Object>(Map.of(
				REALM,REALM_ACTIONS,
				ID,id,
				ACTION,ACTION_ADD));
		new Button(t("add action"),props).addTo(link);

		props.put(ACTION,ACTION_PROPS);
		if (!isEmpty()) {
			Tag ul = new Tag("ol");
			boolean first = true;
			for (Action action : this) {
				props.put(ID, id+"/"+action.id());
				Tag act = new Tag("li").content(action.toString());
				if (!first) {
					props.put(ACTION, ACTION_MOVE);
					new Button("↑",props).addTo(act);
				}
				props.put(ACTION, ACTION_DROP);
				new Button("-",props).addTo(act);
				act.addTo(ul);
				first = false;
			}
			ul.addTo(link);
		}		
	}
	
	private static String t(String text,Object...fills) {
		return Translation.get(Application.class, text, fills);
	}

	public static Object process(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return t("No action list id passed to ActionList.process()!");
		String[] parts = params.get(ID).split("/");
		int listId = Integer.parseInt(parts[0]);
		int actionId = parts.length>1 ? Integer.parseInt(parts[1]) : 0;
		ActionList actionList = actionLists.get(listId);
		if (actionList == null) return t("No action list with id {} found!",listId);
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to ActionList.process()!");
		switch (action) {
			case ACTION_ADD:
				return actionList.addActionForm(params);
			case ACTION_DROP:
				return actionList.drop(actionId) ? t("Action removed") : t("No action with id {} found!",actionId);
			case ACTION_MOVE:
				return actionList.moveUp(actionId) ? t("Action moved") : t("No action with id {} found!",actionId);
		}
		return t("Unknown action: {}",action);
	}

	private Object addActionForm(HashMap<String, String> params) {
		Window win = new Window("add-action-form", t("Add action to action list"));		
		String formId ="add-action-to-"+id;
		Tag typeForm = new Form(formId);
		new Input(REALM, REALM_ACTIONS).hideIn(typeForm);
		new Input(ID,id).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD).hideIn(typeForm);
		String type = params.get(TYPE);
		if (type == null) return actionTypeForm(win);
		
		switch (type) {
			case "FinishRoute":
				add(new FinishRoute());
				break;
			case "SetSignalsToStop":
				add(new SetSignalsToStop());
				break;
			case "TurnTrain":
				add(new TurnTrain());
				break;
			default:
				actionTypeForm(win);
				new Tag("span").content(t("Unknown action type: {}",type)).addTo(win);
				return win;			
		}
		return t("Action added!");
	}

	private Object actionTypeForm(Window win) {
		String formId ="add-action-to-"+id;
		Tag typeForm = new Form(formId);
		new Input(REALM, REALM_ACTIONS).hideIn(typeForm);
		new Input(ID,id).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD).hideIn(typeForm);
		Select select = new Select(TYPE);
		List<Class<? extends Action>> classes = List.of(
				SpeedReduction.class,
				SetSignalsToStop.class,
				FinishRoute.class,
				TurnTrain.class,
				ConditionalAction.class);
		for (Class<? extends Action> clazz : classes) select.addOption(clazz.getSimpleName());
		select.addTo(new Label("Action type:")).addTo(typeForm);
		return new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(typeForm).addTo(win);
	}
}
