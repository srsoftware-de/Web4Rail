package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public abstract class StretchableTile extends Tile {
	private static final String STRETCH_LENGTH = "stretch";
	public int stretch = 1;
	
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		if (stretch != 1) config.put(STRETCH_LENGTH, stretch);
		return config;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (stretch > 1) json.put(STRETCH_LENGTH, stretch);
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(STRETCH_LENGTH)) stretch = json.getInt(STRETCH_LENGTH);
		return super.load(json);
	}
	
	@Override
	public Form propForm(String id) {
		Form form = super.propForm(id);
		new Tag("h4").content(stretchType()).addTo(form);
		
		new Input(STRETCH_LENGTH, stretch).numeric().addTo(new Label(stretchType()+":"+NBSP)).addTo(new Tag("p")).addTo(form);
		
		return form;
	}
	
	private void stretch(String value) {
		try {
			stretch(Integer.parseInt(value));
		} catch (NumberFormatException nfe) {
			LOG.warn("{} is not a valid length!",value);
		}
	}

	public void stretch(int len) {
		this.stretch = Math.max(1, len);
	}
	
	protected abstract String stretchType();
	
	@Override
	public Tile update(HashMap<String, String> params) {
		for (Entry<String, String> entry : params.entrySet()) {
			switch (entry.getKey()) {
				case STRETCH_LENGTH:
					stretch(entry.getValue());
					break;
			}
		}
		return super.update(params);
	}
}
