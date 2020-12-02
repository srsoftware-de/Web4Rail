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
import de.srsoftware.web4rail.tags.Fieldset;
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
	
	public Fieldset list() {
		return list(null);
	}
	
	public Fieldset list(String caption) {
		Fieldset fieldset = new Fieldset(t("Conditions"));
		if (caption != null) new Tag("p").content(caption).addTo(fieldset);
		Tag list = new Tag("ul");
		newConditionForm().addTo(new Tag("li")).addTo(list);
		forEach(condition -> condition.link("li", condition).addTo(list));
		list.addTo(fieldset);
		return fieldset;
	}

	private Form newConditionForm() {
		Form form = new Form("add-condition-form");
		new Input(REALM, REALM_CONDITION).hideIn(form);
		new Input(ACTION,ACTION_ADD).hideIn(form);
		// new Input(CONTEXT,context).hideIn(form); TODO: add context
		Condition.selector().addTo(form);
		new Button(t("Add condition"), form).addTo(form);
		return form;
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

	private static String t(String tx, Object...fills) {
		return Translation.get(Application.class, tx, fills);
	}
}
