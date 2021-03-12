package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public abstract class TurnoutL extends Turnout {

	private static final String LEFT = "left";

	@Override
	public Object click(boolean shift) throws IOException {
		Object o = super.click(shift);
		if (!shift) {
			if (isSet(train)) {
				plan.stream(t("{} is locked by {}!",this,train)); 
			} else state(state == State.STRAIGHT ? State.LEFT : State.STRAIGHT);
		}
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Straight port")+COL,new Input(STRAIGHT, portA).numeric());
		formInputs.add(t("Left port")+COL,new Input(LEFT, portB).numeric());
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public List<State> states() {
		return List.of(State.STRAIGHT,State.LEFT);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(LEFT)) portB = Integer.parseInt(params.get(LEFT));
		return super.update(params);
	}
}
