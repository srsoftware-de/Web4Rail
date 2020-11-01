package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public abstract class Condition implements Constants {
	public static final Logger LOG = LoggerFactory.getLogger(Condition.class);
	private static final String INVERTED = "inverted";
	private static final String PREFIX = Condition.class.getPackageName();
	private static HashMap<Integer, Condition> conditions = new HashMap<Integer, Condition>();
	public abstract boolean fulfilledBy(Context context);
	public boolean inverted = false;
	protected int id;

	public Condition() {
		this(Application.createId());
	}
	
	public Condition(int id) {
		this.id = id;
		conditions.put(id, this);
	}	
	
	public static Object action(HashMap<String, String> params,Plan plan) {
		if (!params.containsKey(ID)) return t("No id passed to Condition.action!");
		int cid = Integer.parseInt(params.get(ID));
		Condition condition = conditions.get(cid);
		if (condition == null) return t("No condition with id {}!",cid);
		
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to Condition.action!");
		
		switch (action) {
			case ACTION_PROPS:
				return condition.properties(params);
			case ACTION_UPDATE:
				condition.update(params);
				return plan.showContext(params);
		}
		return t("Unknown action: {}",action);
	}
	
	public static Condition create(String type) {
		try {
			return (Condition) Class.forName(PREFIX+"."+type).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int id() {
		return id;
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject().put(TYPE, getClass().getSimpleName());
		if (inverted) json.put(INVERTED, true);
		return json;
	}
	
	public Condition load(JSONObject json) {
		inverted = json.has(INVERTED) && json.getBoolean(INVERTED);
		return this;
	}

	public Tag link(String tagClass,String context) {
		String json = new JSONObject(Map.of(REALM,REALM_CONDITION,ID,id,ACTION,ACTION_PROPS,CONTEXT,context)).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(toString());
	}
	
	private static List<Class<? extends Condition>> list() {
		return List.of(TrainHasTag.class,TrainSelect.class,TrainLength.class);
	}
	
	public Tag propForm(HashMap<String, String> params) {
		Form form = new Form("condition-props-"+id);
		new Input(REALM,REALM_CONDITION).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(ID,id).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		return form;
	}

	protected Window properties(HashMap<String, String> params) {
		Window win = new Window("condition-props", t("Properties of {}",getClass().getSimpleName()));		
		Tag form = propForm(params);
		new Checkbox(INVERTED, t("inverted"), inverted).addTo(form);
		new Button(t("Apply"),"return submitForm('condition-props-"+id+"');").addTo(form).addTo(win);
		return win;
	}
	
	public static Tag selector() {
		Select select = new Select(REALM_CONDITION);
		TreeMap<String, String> names = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Class<? extends Condition> clazz : list()) {
			String s = t(clazz.getSimpleName());
			names.put(s, clazz.getSimpleName());
		}
		
		for (Entry<String, String> entry : names.entrySet()) select.addOption(entry.getValue(), entry.getKey());
		return select.addTo(new Label(t("Action type:")+NBSP));
	}
	
	public static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
		
	@Override
	public String toString() {
		return t("invalid condition");
	}

	protected Object update(HashMap<String, String> params) {
		inverted = "on".equals(params.get(INVERTED));
		return t("updated {}.",this);
	}
}
