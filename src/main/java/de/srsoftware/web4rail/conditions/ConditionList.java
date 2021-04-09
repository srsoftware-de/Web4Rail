package de.srsoftware.web4rail.conditions;

import java.util.Iterator;
import java.util.Map;
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
import de.srsoftware.web4rail.tags.Window;

public class ConditionList extends Condition implements Iterable<Condition>{
	
	private static final String CONDITIONS = "conditions";
	private Vector<Condition> conditions = new Vector<Condition>();
	
	public ConditionList add(Condition condition) {
		conditions.add(condition);
		condition.parent(this);
		return this;
	}

	public void addAll(ConditionList conditions) {
		this.conditions.addAll(conditions.conditions);		
	}

	public void clear() {
		conditions.clear();
	}
	
	public boolean fulfilledBy(Context context) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return false;
		}
		return true;
	}
	
	protected String glue() {
		return t("and");
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
		JSONObject json = super.json();
		JSONArray jConditions = new JSONArray();
		conditions.stream().map(Condition::json).forEach(js -> jConditions.put(js));
		json.put(CONDITIONS, jConditions);
		return json;
	}
	
	public Fieldset list() {
		return list(null);
	}
	
	public Fieldset list(String caption) {
		Fieldset fieldset = new Fieldset(t("Conditions"));
		if (caption != null) new Tag("p").content(caption).addTo(fieldset);
		listInternal().addTo(fieldset);
		return fieldset;
	}
	
	private Tag listInternal() {
		Tag list = new Tag("ul");
		for (Condition condition : conditions) {
			Tag item = new Tag("li");
			condition.link("span", condition).addTo(item);
			condition.button(t("delete"), Map.of(ACTION,ACTION_DROP)).addTo(item.content(NBSP)).addTo(list);
			if (condition instanceof ConditionList) {
				((ConditionList)condition).listInternal().addTo(item);
			}						
		}
		newConditionForm().addTo(new Tag("li")).addTo(list);
		return list;
		
	}

	@Override
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(CONDITIONS)) {
			JSONArray jConditions = json.getJSONArray(CONDITIONS);
			for (Object o : jConditions) {
				if (o instanceof JSONObject) {
					JSONObject jo = (JSONObject) o;
					Condition condition = Condition.create(jo.getString(TYPE));
					if (isSet(condition)) add(condition.load(jo));
				}
			}
		}
		return this;
	}

	private Form newConditionForm() {
		Form form = new Form("new-condition-form-"+id());
		new Input(REALM, REALM_CONDITION).hideIn(form);
		new Input(ACTION,ACTION_ADD).hideIn(form);
		new Input(ID,id()).hideIn(form);
		Condition.selector().addTo(form);
		new Button(t("Add condition"), form).addTo(form);
		return form;
	}
	
	@Override
	public Window properties(String...errors) {
		BaseClass parent = parent();
		if (isSet(parent)) return parent.properties(errors);
		return super.properties(errors);
	}
	
	@Override
	public BaseClass remove() {
		LOG.debug("Removing Condition List ({}) {}",id(),this);
		super.remove();
		while (!conditions.isEmpty()) conditions.lastElement().remove();
		return this;
	}
	
	public boolean remove(Object condition) {
		return conditions.remove(condition);
	}
	
	@Override
	public void removeChild(BaseClass child) {
		conditions.remove(child);
		super.removeChild(child);
	}


	public Stream<Condition> stream() {
		return conditions.stream();
	}
	
	@Override
	public String toString() {
		if (conditions.isEmpty()) return "["+t("Click here to add conditions")+"]";
		StringBuffer sb = new StringBuffer(conditions.firstElement().toString());
		String glue = glue();
		for (int i=1; i<conditions.size(); i++) {
			Condition condition = conditions.get(i);
			String cString = condition instanceof ConditionList ? " ("+condition+")" : " "+condition;
			sb.append(" ").append(glue).append(cString);
		}
		return sb.toString();
	}
}
