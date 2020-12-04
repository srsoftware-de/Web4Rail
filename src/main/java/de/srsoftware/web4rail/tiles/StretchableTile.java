package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public abstract class StretchableTile extends Tile {
	private static final String STRETCH_LENGTH = "stretch";
	private int stretch = 1;
	private Vector<Id> shadows = new Vector<Id>();
	
	public void add(Shadow shadow) {
		shadows.add(shadow.id());
	}
	
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

		boolean moved = super.move(dx, dy);
		if (moved) placeShadows();
		return moved;
	}
	
	public void placeShadows() {
		removeShadows();
		for (int dx=1; dx<width(); dx++) plan.place(new Shadow(this, x+dx, y));
		for (int dy=1; dy<height(); dy++) plan.place(new Shadow(this, x, y+dy));
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(stretchType(),new Input(STRETCH_LENGTH, stretch).numeric().addTo(new Tag("span")).content(NBSP+t("Tile(s)")));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public BaseClass remove() {
		super.remove();
		removeShadows();
		return this;
	}
	
	private void removeShadows() {
		while (!shadows.isEmpty()) {
			Tile tile = BaseClass.get(shadows.remove(0));
			if (tile instanceof Shadow) tile.remove();
		}
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
