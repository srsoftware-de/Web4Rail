package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TurnoutR extends Turnout {
	
	private static final String RIGHT = "right";

	@Override
	public Object click() throws IOException {
		Object o = super.click();
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else state(state == State.STRAIGHT ? State.RIGHT : State.STRAIGHT);
		return o;
	}
	
	@Override
	public String commandFor(State newState) {

		switch (newState) {
		case RIGHT:			
			return "SET {} GA "+address+" "+portB+" 1 "+delay;
		case STRAIGHT:
			return "SET {} GA "+address+" "+portA+" 1 "+delay;
		default:
			throw new IllegalStateException();
		}
	}
	
	@Override
	public Tag propForm(String id) {
		Tag form = super.propForm(id);
		Tag fieldset = null;
		for (Tag child : form.children()) {
			if (child.is(Fieldset.TYPE)) {
				fieldset = child;
				break;
			}
		}
		new Input(STRAIGHT, portA).numeric().addTo(new Label(t("Straight port:")+NBSP)).addTo(fieldset);
		new Input(RIGHT, portB).numeric().addTo(new Label(t("Right port:")+NBSP)).addTo(fieldset);
		return form;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(RIGHT)) portB = Integer.parseInt(params.get(RIGHT));
		return super.update(params);
	}
}
