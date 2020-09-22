package de.srsoftware.web4rail.tiles;

import java.io.IOException;
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
	public JSONObject json() {
		JSONObject json = super.json();
		if (length > 1) json.put(LENGTH, length);
		return json;
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		return this;
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();

		Tag label = new Tag("label").content(t("length:"));
		new Tag("input").attr("type", "number").attr("name","length").attr("value", length).addTo(label);		
		label.addTo(new Tag("p")).addTo(form);
		
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
	public Tile update(HashMap<String, String> params) throws IOException {
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
