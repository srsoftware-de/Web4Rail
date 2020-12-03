package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(stretchType(),new Input(STRETCH_LENGTH, stretch).numeric().addTo(new Tag("span")).content(NBSP+t("Tile(s)")));
		return super.properties(preForm, formInputs, postForm);
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
