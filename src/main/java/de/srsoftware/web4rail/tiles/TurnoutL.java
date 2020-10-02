package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TurnoutL extends Turnout {

	private static final String LEFT = "left";

	@Override
	public Object click() throws IOException {
		LOG.debug("TurnoutL.click()");
		Object o = super.click();
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else state(state == State.STRAIGHT ? State.LEFT : State.STRAIGHT);
		return o;
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		Tag fieldset = null;
		for (Tag child : form.children()) {
			if (child.is(Fieldset.TYPE)) {
				fieldset = child;
				break;
			}
		}
		new Input(STRAIGHT, portA).numeric().addTo(new Label(t("Straight port"))).addTo(fieldset);
		new Input(LEFT, portB).numeric().addTo(new Label(t("Left port"))).addTo(fieldset);
		return form;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(LEFT)) portB = Integer.parseInt(params.get(LEFT));
		return super.update(params);
	}
	
	@Override
	public void state(State newState) throws IOException {
		init();
		LOG.debug("Setting {} to {}",this,newState);
		int p = 0;
		switch (newState) {
		case LEFT:
			p = portB;
			break;
		case STRAIGHT:
			p = portA;
			break;
		default:
		}
		if (p != 0) plan.queue("SET {} GA "+address+" "+p+" 1 "+delay);
		state = newState;
		plan.stream("place "+tag(null));
	}
}
