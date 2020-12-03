package de.srsoftware.web4rail.conditions;

import java.util.Iterator;
import java.util.Vector;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;

public class ConditionList extends Condition implements Iterable<Condition>{
	
	private Vector<Condition> conditions = new Vector<Condition>();
	
	public void add(Condition condition) {
		conditions.add(condition);
	}

	public void addAll(ConditionList conditions) {
		this.conditions.addAll(conditions.conditions);		
	}

	
	public boolean fulfilledBy(Context context) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return false;
		}
		return true;
	}
	
	public boolean isEmpty() {
		return conditions.isEmpty();
	}
	
	@Override
	public Iterator<Condition> iterator() {
		return conditions.iterator();
	}
	
	@Override
	public JSONObject json() {
		String cls = getClass().getSimpleName();
		throw new UnsupportedOperationException(cls+".json() not supported, use "+cls+".jsonArray instead!");
	}

	public JSONArray jsonArray() {
		JSONArray json = new JSONArray();
		for (Condition condition : conditions) json.put(condition.json());
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
		conditions.forEach(condition -> condition.link("li", condition).addTo(list));
		list.addTo(fieldset);
		return fieldset;
	}

	public void load(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			Condition condition = Condition.create(json.getString(TYPE));			
			if (condition != null) {
				condition.parent(this);
				conditions.add(condition.load(json));
			}
		}
	}

	private Form newConditionForm() {
		Form form = new Form("add-condition-form");
		new Input(REALM, REALM_CONDITION).hideIn(form);
		new Input(ACTION,ACTION_ADD).hideIn(form);
		new Input(PARENT,id());
		Condition.selector().addTo(form);
		new Button(t("Add condition"), form).addTo(form);
		return form;
	}
	
	@Override
	public BaseClass remove() {
		super.remove();
		while (!conditions.isEmpty()) conditions.lastElement().remove();
		return this;
	}
	
	public boolean remove(Object condition) {
		return conditions.remove(condition);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		conditions.remove(child);
	}


	public Stream<Condition> stream() {
		return conditions.stream();
	}
}
