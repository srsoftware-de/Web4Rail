package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.conditions.ConditionList;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public class ConditionalAction extends ActionList {
	
	private static final String CONDITIONS = "conditions";
	private static final String ELSE_ACTONS = "else_actions";
	private ConditionList conditions = new ConditionList();
	private ActionList elseActions;

	public ConditionalAction(BaseClass parent) {
		super(parent);
		conditions.parent(this);
		elseActions = new ActionList(parent);
	}

	public boolean equals(ConditionalAction other) {
		return (conditions+":"+actions).equals(other.conditions+":"+other.actions);
	}
		
	@Override
	public boolean fire(Context context,Object cause) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return elseActions.fire(context, cause);
		}
		return super.fire(context.clone(),cause); // actions, that happen within the conditional action list must not modify the global context.
	}
	
	@Override
	public Integer getSpeed(Context context) {
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return null; 
		}
		return super.getSpeed(context);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		JSONArray conditions = new JSONArray();
		for (Condition condition : this.conditions) conditions.put(condition.json());
		json.put(CONDITIONS, conditions);
		if (!elseActions.isEmpty()) json.put(ELSE_ACTONS, elseActions.json());
		return json;
	}
	
	@Override
	public <T extends Tag> T listAt(T parent) {
		T tag = super.listAt(parent);
		if (!elseActions.isEmpty()) {
			Tag div = new Tag("div").clazz("else");
			new Tag("span").content(t("else:")+NBSP).addTo(div);
			elseActions.listAt(div);
			div.addTo(tag);
		}
		return tag;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);		
		if (json.has(CONDITIONS)) {
			conditions.clear();
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
		if (json.has(ELSE_ACTONS)) elseActions.load(json.getJSONObject(ELSE_ACTONS));
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		preForm.add(conditions.list());
		Window win = super.properties(preForm, formInputs, postForm,errors);	
		Optional<Fieldset> actionFieldSet = win.children()
			.stream()
			.filter(tag -> tag instanceof Fieldset)
			.map(tag -> (Fieldset)tag)
			.filter(fs -> "actions".equals(fs.get("class")))
			.findFirst();
		
		if (actionFieldSet.isPresent()) {
			Vector<Tag> children = actionFieldSet.get().children();
			children.insertElementAt(new Tag("h3").content(t("Actions in case conditions are fulfilled")),1);
			LOG.debug("children: "+children);
			Optional<Tag> elseTag = children.stream().filter(tag -> "else".equals(tag.get("class"))).findFirst();
			if (elseTag.isPresent()) {
				children = elseTag.get().children();
				children.remove(0);
				children.insertElementAt(new Tag("h3").content(t("Actions in case conditions are <em>not</em> fulfilled")),0);
			} else {
				children.add(new Tag("h3").content(t("Actions in case conditions are <em>not</em> fulfilled")));
				elseActions.listAt(actionFieldSet.get());
			}
		}
		
		return win;
		
	}

	@Override
	public void removeChild(BaseClass child) {
		conditions.remove(child);
		super.removeChild(child);
	}

	@Override
	public String toString() {
		if (conditions.isEmpty()) return "["+t("Click here to add conditions")+"]";
		return t("if ({})",conditions)+": ";
	}

	@Override
	protected Object update(Params params) {
		String conditionClass = params.getString(REALM_CONDITION);
		Condition condition = Condition.create(conditionClass);
		if (isNull(condition)) return t("Unknown type of condition: {}",conditionClass);
		condition.parent(this);
		conditions.add(condition);
		return super.update(params);
	}
}
