package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;

public abstract class StretchableTile extends Tile {
	private static final String LENGTH = "length";
	public int length = 1;
	
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		if (length != 1) config.put(LENGTH, length);
		return config;
	}
	
	@Override
	public void configure(JSONObject config) {
		super.configure(config);
		if (config.has(LENGTH)) setLength(config.getInt(LENGTH));
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();

		Tag label = new Tag("label").content(t("length:"));
		new Tag("input").attr("type", "number").attr("name","length").attr("value", length).addTo(label);		
		label.addTo(form);
		
		return form;
	}
	
	private void setLength(String value) {
		try {
			setLength(Integer.parseInt(value));
		} catch (NumberFormatException nfe) {
			LOG.warn("{} is not a valid length!",value);
		}
	}

	public void setLength(int len) {
		this.length = Math.max(1, len);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		super.update(params);
		for (Entry<String, String> entry : params.entrySet()) {
			switch (entry.getKey()) {
				case LENGTH:
					setLength(entry.getValue());
					break;
			}
		}
		return this;
	}
}
