package de.srsoftware.web4rail.functions;

import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.moving.Locomotive;

/**
 * @author Stephan Richter
 *
 */
public class FunctionList extends HashSet<Function> implements Constants{

	private static final long serialVersionUID = 8013610745085726979L;
	private HashSet<String> enabledFunctions = new HashSet<>();
	
	public boolean enabled(String name) {
		return enabledFunctions.contains(name);
	}


	public JSONArray json() {
		JSONArray json = new JSONArray();
		for (Function fun : this) json.put(fun.json());
		return json;
	}

	public void load(JSONArray arr, Locomotive loco) {
		arr.forEach(o -> {
			if (o instanceof JSONObject) load((JSONObject)o,loco);
		});
	}

	private void load(JSONObject json, Locomotive loco) {
		if (json.has(TYPE)) add(Function.create(json.getString(TYPE)).load(json).parent(loco));
	}

	public FunctionList toggle(String name) {
		boolean enabled = !enabledFunctions.remove(name);
		if (enabled) enabledFunctions.add(name);
		stream().filter(fun -> name.equals(fun.name())).forEach(fun -> fun.setState(enabled));
		return this;
	}
}
