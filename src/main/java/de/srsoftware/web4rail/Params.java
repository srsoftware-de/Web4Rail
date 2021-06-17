package de.srsoftware.web4rail;

import java.util.HashMap;

public class Params extends HashMap<String, Object> {

	private static final long serialVersionUID = 5932558155771729L;
	
	public String getString(Object key) {
		Object v = super.get(key);
		if (v instanceof String) return (String) v;
		if (v == null) return null;
		return v.toString();
	}
	
	public Integer getInt(String key) {
		try {
			return Integer.parseInt(getString(key));
		} catch (Exception e) {
			return null;
		}
	}
	
	public Long getLong(String key) {
		try {
			return Long.parseLong(getString(key));
		} catch (Exception e) {
			return null;
		}
	}

	public Params getParams(Object key) {
		Object v = super.get(key);
		if (v instanceof Params) return (Params) v;		
		return null;
	}
}
