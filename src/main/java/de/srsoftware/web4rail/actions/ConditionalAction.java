package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.conditions.TrainSelect;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;

public class ConditionalAction extends Action {
	
	private static final String CONDITIONS = "conditions";
	private static final String ACTIONS = "actions";
	private Vector<Condition> conditions = new Vector<Condition>();
	private ActionList actions = new ActionList();
	
	private Tag actionsForm(HashMap<String, String> params) {
		Fieldset fieldset = new Fieldset(t("Actions"));
		actions.addTo(fieldset, params.get(CONTEXT));
		return fieldset;
	}
	
	public ActionList children() {
		return actions;
	}
	
	private Tag conditionForm(HashMap<String, String> params) {
		Fieldset fieldset = new Fieldset(t("Conditions"));

		if (!conditions.isEmpty()) {
			Tag list = new Tag("ul");
			for (Condition condition : conditions) condition.link("li",params.get(CONTEXT)).addTo(list);
			list.addTo(fieldset);
		}

		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);

		Select select = new Select(REALM_CONDITION);
		List<Class<? extends Condition>> classes = List.of(TrainSelect.class);
		for (Class<? extends Condition> clazz : classes) select.addOption(clazz.getSimpleName());
		select.addTo(form);
		return new Button(t("Add condition"),form).addTo(form).addTo(fieldset);
	}
		
	@Override
	public boolean fire(Context context) throws IOException {
		for (Condition condition : conditions) {
			if (condition.fulfilledBy(context) != condition.inverted) return actions.fire(context);
		}
		return false;		
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONArray conditions = new JSONArray();
		for (Condition condition : this.conditions) conditions.put(condition.json());
		json.put(CONDITIONS, conditions);
		json.put(ACTIONS, actions.json());
		return json;
	}
	
	public static ConditionalAction load(JSONObject json) {
		ConditionalAction action = new ConditionalAction();
		for (Object o : json.getJSONArray(CONDITIONS)) {
			if (o instanceof JSONObject) {
				Condition condition = Condition.load((JSONObject)o);
				if (condition != null) action.conditions.add(condition);
			}
		}
		action.actions = ActionList.load(json.getJSONArray(ACTIONS));
		return action;
	}
		
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		conditionForm(params).addTo(win);
		actionsForm(params).addTo(win);
		return win;
	}

	@Override
	public String toString() {
		if (conditions.isEmpty()) return t("[Click here to add condition]");
		StringBuffer sb = new StringBuffer();		
		for (int i = 0; i<conditions.size(); i++) {
			if (i>0) sb.append(t(" or "));
			sb.append(conditions.get(i).toString());
		}
		return t("if ({}):",sb);
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String conditionClass = params.get(REALM_CONDITION);
		if (conditionClass != null) {
			switch (conditionClass) {
			case "TrainSelect":
				conditions.add(new TrainSelect());
				break;

			default:
				return t("Unknown type of condition: {}",conditionClass);
			}
		}
		return super.update(params);
	}
}
