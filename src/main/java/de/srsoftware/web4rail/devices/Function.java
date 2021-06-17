package de.srsoftware.web4rail.devices;

import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;

public class Function extends BaseClass{
	protected static final Logger LOG = LoggerFactory.getLogger(Function.class);
	
	public static final String DIRECTIONAL = "directional";
	public static final String FORWARD = "forward";
	public static final String REVERSE = "reverse";

	
	private boolean directional;
	private boolean reverse;
	private boolean forward;
	private String type;
	private String name;

	public Function(String type, String name, Params dirs) {
		this.type = type;
		this.name = name;
		for (Entry<String, Object> entry : dirs.entrySet()) setDirection(entry.getKey(), "on".equals(entry.getValue()));
	}

	public Function(JSONObject json) {
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(TYPE)) type = json.getString(TYPE);
		if (json.has(DIRECTION)) json.getJSONArray(DIRECTION).forEach(o -> setDirection(o.toString(), true));
	}

	public boolean isDirectional() {
		return directional;
	}

	public boolean isForward() {
		return forward;
	}

	public boolean isReverse() {
		return reverse;
	}



	public JSONObject json() {
		JSONArray directions = new JSONArray();
		if (directional) directions.put(DIRECTIONAL);
		if (forward) directions.put(FORWARD);
		if (reverse) directions.put(REVERSE);
		
		JSONObject json = new JSONObject();
		if (!directions.isEmpty()) json.put(DIRECTION, directions);
		json.put(NAME, name);
		if (isSet(type)) json.put(TYPE, type);
		return json;
	}
	
	public static JSONObject json(HashMap<String, HashMap<Integer, Function>> functions) {
		JSONObject json = new JSONObject();
		for (Entry<String, HashMap<Integer, Function>> entry : functions.entrySet()) {
			HashMap<Integer, Function> map = entry.getValue();
			if (map.isEmpty()) continue;
			String type = entry.getKey();
			JSONObject list = new JSONObject();
			for (Integer idx : map.keySet()) list.put(idx.toString(), map.get(idx).json());
			json.put(type, list);
		}
		return json;
	}
	
	public String name() {
		return name;
	}
	
	private void setDirection(String key,boolean value) {
		switch (key) {
			case DIRECTIONAL:
				directional = value;
				break;
			case FORWARD:
				forward = value;
				break;
			case REVERSE:
				reverse = value;
				break;
			default:
				LOG.debug("unknwon direction {}",key);
		}
	}
	
	@Override
	public String toString() {
		return type+"("+(forward?BaseClass.t("forward"):"")+(reverse?" "+BaseClass.t("reverse"):"")+(directional?" "+BaseClass.t("directional"):"").trim()+")";
	}
	


}
