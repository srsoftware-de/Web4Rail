package de.srsoftware.web4rail;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;

public class BaseClass implements Constants{
	
	public static Tag link(String tagClass,Map<String,Object> params,Object caption) {
		String json = new JSONObject(params).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(caption.toString());
	}
		
	public static boolean isNull(Object o) {
		return o==null;
	}

	public static boolean isSet(Object o) {
		return o != null;
	}
	
	public static HashMap<String, String> merged(Map<String, String> base, Map<String, String> overlay) {
		HashMap<String,String> merged = new HashMap<>(base);
		overlay.entrySet().stream().forEach(entry -> merged.put(entry.getKey(), entry.getValue()));
		return merged;
	}
}
