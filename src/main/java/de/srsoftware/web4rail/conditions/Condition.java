package de.srsoftware.web4rail.conditions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;

public abstract class Condition implements Constants {

	private static HashMap<Integer, Condition> conditions = new HashMap<Integer, Condition>();
	
	public abstract boolean fulfilledBy(Context context);
	protected int id;
	
	public static Object action(HashMap<String, String> params) {
		if (!params.containsKey(ID)) return t("No id passed to Condition.action!");
		int cid = Integer.parseInt(params.get(ID));
		Condition condition = conditions.get(cid);
		if (condition == null) return t("No condition with id {}!",cid);
		
		String action = params.get(ACTION);
		if (action == null) return t("No action passed to Condition.action!");
		
		switch (action) {
			case ACTION_PROPS:
				return condition.properties();
			case ACTION_UPDATE:
				return condition.update(params);
		}
		return t("Unknown action: {}",action);
	}

	protected abstract Window properties();


	public Condition() {
		this(new Date().hashCode());
	}
	
	public Condition(int id) {
		this.id = id;
		conditions.put(id, this);
	}
	
	public Tag link(String tagClass) {
		String json = new JSONObject(Map.of(REALM,REALM_CONDITION,ID,id,ACTION,ACTION_PROPS)).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(toString());
	}

	
	@Override
	public String toString() {
		return t("invalid condition");
	}
	
	public static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
	
	protected abstract Object update(HashMap<String, String> params);
}
