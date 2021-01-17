package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public abstract class StretchableTile extends TileWithShadow {
	private static final String STRETCH_LENGTH = "stretch";
	private int stretch = 1;
	
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
	public boolean move(int dx, int dy) {
		for (int destX=1; destX<width(); destX++) {
			int destY = dy > 0 ? height() : dy;
			Tile tileAtDest = plan.get(Tile.id(x+destX, y+destY), true);
			if (isNull(tileAtDest) || tileAtDest == this) continue;
			if (!tileAtDest.move(dx, dy)) return false;
		}

		for (int destY=1; destY<height(); destY++) {
			int destX = dx > 0 ? width() : dx;
			Tile tileAtDest = plan.get(Tile.id(x+destX, y+destY), true);
			if (isNull(tileAtDest) || tileAtDest == this) continue;
			if (!tileAtDest.move(dx, dy)) return false;
		}

		return super.move(dx, dy);
	}
		
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(stretchType(),new Input(STRETCH_LENGTH, stretch).numeric().addTo(new Tag("span")).content(NBSP+t("Tile(s)")));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public BaseClass remove() {
		LOG.debug("Removing stretchable Tile ({}) {}",id(),this);
		removeShadows();
		return super.remove();
	}

	public int stretch() {
		return stretch;
	}
	
	private void stretch(String value) {
		try {
			stretch(Integer.parseInt(value));
		} catch (NumberFormatException nfe) {
			LOG.warn("{} is not a valid length!",value);
		}
	}

	public void stretch(int newStretch) {
		newStretch = Math.max(1, newStretch);
		if (newStretch != stretch) {
			stretch = newStretch;
			placeShadows();
		}
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
