package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.conditions.ConditionList;
import de.srsoftware.web4rail.tags.Fieldset;

public class ConditionalAction extends ActionList {
	
	private static final String CONDITIONS = "conditions";
	private ConditionList conditions = new ConditionList();

	public ConditionalAction(BaseClass parent) {
		super(parent);
		conditions.parent(this);
	}

	public boolean equals(ConditionalAction other) {
		return (conditions+":"+actions).equals(other.conditions+":"+other.actions);
	}
		
	@Override
	public boolean fire(Context context) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return true; // wenn die Bedingung nicht erfüllt ist, ist das kein Fehler!
		}
		return super.fire(context.clone()); // actions, that happen within the conditional action list must not modify the global context.
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONArray conditions = new JSONArray();
		for (Condition condition : this.conditions) conditions.put(condition.json());
		json.put(CONDITIONS, conditions);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(CONDITIONS)) {
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
		}
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		preForm.add(conditions.list());
		return super.properties(preForm, formInputs, postForm);
		
	}

	@Override
	public void removeChild(BaseClass child) {
		conditions.remove(child);
		super.removeChild(child);
	}

	@Override
	public String toString() {
		if (conditions.isEmpty()) return "["+t("Click here to add conditions")+"]";
		return t("if ({}):",conditions);
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
