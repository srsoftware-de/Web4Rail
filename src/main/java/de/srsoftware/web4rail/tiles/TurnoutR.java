package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public abstract class TurnoutR extends Turnout {
	
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Straight port")+COL,new Input(STRAIGHT, portA).numeric());
		formInputs.add(t("Right port")+COL,new Input(RIGHT, portB).numeric());
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public List<State> states() {
		return List.of(State.STRAIGHT,State.RIGHT);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(RIGHT)) portB = Integer.parseInt(params.get(RIGHT));
		return super.update(params);
	}
}
