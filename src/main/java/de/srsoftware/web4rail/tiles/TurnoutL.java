package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public abstract class TurnoutL extends Turnout {

	private static final String LEFT = "left";

	@Override
	public Object click() throws IOException {
		Object o = super.click();
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else state(state == State.STRAIGHT ? State.LEFT : State.STRAIGHT);
		return o;
	}
	
	@Override
	protected String commandFor(State newState) {
		switch (newState) {
		case LEFT:			
			return "SET {} GA "+address+" "+portB+" 1 "+delay;
		case STRAIGHT:
			return "SET {} GA "+address+" "+portA+" 1 "+delay;
		default:
			throw new IllegalStateException();
		}
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Straight port:"),new Input(STRAIGHT, portA).numeric());
		formInputs.add(t("Left port:"),new Input(LEFT, portB).numeric());
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(LEFT)) portB = Integer.parseInt(params.get(LEFT));
		return super.update(params);
	}
}
