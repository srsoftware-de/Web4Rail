package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.stream.Collectors;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;

public class OrCondition extends Condition{
	
	private static final String CONDITIONS = "conditions";
	private ConditionList conditions = new ConditionList();

	@Override
	public boolean fulfilledBy(Context context) {
		for (Condition condition : conditions) {
			if (condition.fulfilledBy(context)) return true;
		}
		return false;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(CONDITIONS, conditions.json());
	}
	
	@Override
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(CONDITIONS)) conditions.load(json.getJSONArray(CONDITIONS));
		return this;
	}
	
	@Override
	protected Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		
		win.children().insertElementAt(conditions.tag(REALM_CONDITION+":"+id()),2);
		return win;
	}
	
	@Override
	public String toString() {
		return conditions.isEmpty() ? t("Click here to select conditions") : String.join(" "+t("OR")+" ", conditions.stream().map(Object::toString).collect(Collectors.toList()));
	}
}
