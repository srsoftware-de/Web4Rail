package de.srsoftware.web4rail.conditions;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass.Context;
import de.srsoftware.web4rail.BaseClass.Id;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;

public class ConditionList extends Vector<Condition> implements Constants{
	
	private static final long serialVersionUID = 5826717120751473807L;

	public boolean fulfilledBy(Context context) {
		for (Condition condition : this) {
			if (!condition.fulfilledBy(context)) return false;
		}
		return true;
	}

	public JSONArray json() {
		JSONArray json = new JSONArray();
		for (Condition condition : this) json.put(condition.json());
		return json;
	}

	public void load(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			Condition condition = Condition.create(json.getString(TYPE));
			if (condition != null) add(condition.parent(this).load(json));
		}
	}

	public void removeById(Id cid) {
		for (Condition condition : this) {
			if (condition.id().equals(cid)) {
				remove(condition);
				break;
			}
		}	
	}

	public Tag tag(String context) {
		if (context != null) {

			Tag list = new Tag("ul");
			for (Condition condition : this) {
				condition.link(condition.toString(),"li",context).addTo(list);
			}
			Tag div = list.addTo(new Tag("div"));
		
			Form form = new Form("add-condition-form");
			new Input(REALM, REALM_CONDITION).hideIn(form);
			new Input(ACTION,ACTION_ADD).hideIn(form);
			new Input(CONTEXT,context).hideIn(form);
			Condition.selector().addTo(form);
			new Button(t("Add condition"), form).addTo(form).addTo(div);
			
			return div;
		}
		return null;
		
	}

	private static String t(String tx, Object...fills) {
		return Translation.get(Application.class, tx, fills);
	}
}
