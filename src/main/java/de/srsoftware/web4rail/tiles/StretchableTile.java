package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Form;

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
	
	public Tag propMenu() {
		Window menu = new Window("tile-properties",t("Properties of {} @ ({},{})",getClass().getSimpleName(),x,y));
		Form form = new Form();
		new Tag("input").attr("type", "hidden").attr("name","action").attr("value", "update").addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","x").attr("value", x).addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","y").attr("value", y).addTo(form);

		Tag label = new Tag("label").content(t("length:"));
		new Tag("input").attr("type", "number").attr("name","length").attr("value", length).addTo(label);		
		label.addTo(form);
		
		new Tag("button").attr("type", "submit").content(t("save")).addTo(form);
		form.addTo(menu);
		return menu;
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
