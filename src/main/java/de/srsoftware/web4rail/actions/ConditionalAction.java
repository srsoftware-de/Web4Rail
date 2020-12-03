package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;

public class ConditionalAction extends Action {
	
	private static final String CONDITIONS = "conditions";
	private static final String ACTIONS = "actions";
	private Vector<Condition> conditions = new Vector<Condition>();
	private ActionList actions;

	public ConditionalAction(BaseClass parent) {
		super(parent);
		actions = new ActionList(this);
	}

	public ActionList children() {
		return actions;
	}
	
	private StringBuffer conditions() {
		StringBuffer sb = new StringBuffer();		
		for (int i = 0; i<conditions.size(); i++) {
			if (i>0) sb.append(t(" and "));
			sb.append(conditions.get(i).toString());
		}
		return sb;
	}
	
	private Fieldset conditionForm() {
		Fieldset fieldset = new Fieldset(t("Conditions"));

		new Tag("p").content(t("Actions will only fire, if all conditions are fullfilled.")).addTo(fieldset);
		if (!conditions.isEmpty()) {
			Tag list = new Tag("ul");
			for (Condition condition : conditions) {
				Tag li = link("span", condition+NBSP,Map.of()).addTo(new Tag("li"));
				HashMap<String,Object> props = new HashMap<String, Object>(Map.of(REALM,REALM_CONDITION,ID,condition.id(),ACTION,ACTION_DROP,CONTEXT, REALM_ACTIONS+":"+id()));
				new Button(t("delete"), props).addTo(li).addTo(list);
			}
			list.addTo(fieldset);
		}

		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);

		Condition.selector().addTo(form);
		
		new Button(t("Add condition"),form).addTo(form);
		button(t("Back")).addTo(form).addTo(fieldset);
		return fieldset;
	}

	public boolean equals(ConditionalAction other) {
		return (conditions()+":"+actions).equals(other.conditions()+":"+other.actions);
	}
		
	@Override
	public boolean fire(Context context) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return true;
		}
		return actions.fire(context.clone()); // actions, that happen within the conditional action list must not modify the global context.
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONArray conditions = new JSONArray();
		for (Condition condition : this.conditions) conditions.put(condition.json());
		json.put(CONDITIONS, conditions);
		json.put(ACTIONS, actions.jsonArray());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		for (Object o : json.getJSONArray(CONDITIONS)) {
			if (o instanceof JSONObject) {
				JSONObject j = (JSONObject) o;
				Condition condition = Condition.create(j.getString(TYPE));				
				if (isSet(condition)) {
					condition.parent(this);
					conditions.add(condition.load(j));
				}
			}
		}
		actions.load(json.getJSONArray(ACTIONS));
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		preForm.add(conditionForm());
		Fieldset fieldset = new Fieldset(t("Actions"));
		actions.list().addTo(fieldset);
		postForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm);
		
	}

	public ConditionalAction remove(Condition condition) {
		conditions.remove(condition);
		return this;
	}

	@Override
	public String toString() {
		if (conditions.isEmpty()) return t("[Click here to add condition]");
		return t("if ({}):",conditions());
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String conditionClass = params.get(REALM_CONDITION);
		Condition condition = Condition.create(conditionClass);
		if (isNull(condition)) return t("Unknown type of condition: {}",conditionClass);
		condition.parent(this);
		conditions.add(condition);
		return super.update(params);
	}
}
