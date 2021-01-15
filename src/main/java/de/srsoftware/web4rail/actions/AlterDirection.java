package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;

public class AlterDirection extends Action{
	
	enum NEWDIR {
		EAST, WEST, NORTH, SOUTH, REVERSE, TURN
	}

	private static final String NEW_DIRECTION = "new_dir";
	private NEWDIR newDir = NEWDIR.REVERSE;

	public AlterDirection(BaseClass parent) {
		super(parent);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean fire(Context context) {
		Train train = context.train();
		if (isNull(train)) return false;
		
		if (isNull(train.direction())) {
			switch (newDir) {
				case EAST:
					train.heading(Direction.EAST);
					return true;
				case WEST:
					train.heading(Direction.WEST);
					return true;
				case NORTH:
					train.heading(Direction.NORTH);
					return true;
				case SOUTH:
					train.heading(Direction.SOUTH);
					return true;
			}
		}

		switch (newDir) {
		case REVERSE:
			train.reverse();
			return true;
		case TURN:
			train.turn();
			return true;
		}
		
		if (newDir == NEWDIR.valueOf(train.direction().inverse().toString())) train.turn();
		return (newDir == NEWDIR.valueOf(train.direction().toString())); // train already has correct direction
		
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NEW_DIRECTION, newDir);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(NEW_DIRECTION)) newDir = NEWDIR.valueOf(json.getString(NEW_DIRECTION));
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag radios = new Tag("div");
		for (NEWDIR d : NEWDIR.values()) {
			new Radio(NEW_DIRECTION, d, t("{}",d), newDir == d).addTo(radios);	
		}
		
		formInputs.add(t("new direction"),radios);
		return super.properties(preForm, formInputs, postForm);
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public String toString() {
		switch (newDir) {
		case REVERSE:
			return t("reverse train");
		case TURN:
			return t("turn train");
		default:
			return t("Set direction of train to {}",newDir);
		}
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(NEW_DIRECTION)) {
			newDir = NEWDIR.valueOf(params.get(NEW_DIRECTION));
		}
		return super.update(params);
	}
}
