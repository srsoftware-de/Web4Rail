package de.srsoftware.web4rail.devices;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Params;

public class Function implements Constants{
	protected static final Logger LOG = LoggerFactory.getLogger(Function.class);
	
	public static final String DIRECTIONAL = "directional";
	public static final String FORWARD = "forward";
	public static final String REVERSE = "reverse";

	
	private boolean directional;
	private boolean reverse;
	private boolean forward;
	private String type;

	public Function(String type, Params dirs) {
		this.type = type;
		for (Entry<String, Object> entry : dirs.entrySet()) setDirection(entry.getKey(), "on".equals(entry.getValue()));
	}

	public Function(List<Object> list) {
		for (Object item : list) setDirection(item.toString(), true);
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

	public boolean isDirectional() {
		return directional;
	}

	public boolean isForward() {
		return forward;
	}

	public boolean isReverse() {
		return reverse;
	}
	
	public JSONArray json() {
		JSONArray json = new JSONArray();
		if (directional) json.put(DIRECTIONAL);
		if (forward) json.put(FORWARD);
		if (reverse) json.put(REVERSE);
		return json;
	}
	
	@Override
	public String toString() {
		return type+"("+(forward?BaseClass.t("forward"):"")+(reverse?" "+BaseClass.t("reverse"):"")+(directional?" "+BaseClass.t("directional"):"").trim()+")";
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
}
