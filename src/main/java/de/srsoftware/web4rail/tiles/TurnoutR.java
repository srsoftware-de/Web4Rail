package de.srsoftware.web4rail.tiles;

import java.io.IOException;

public class TurnoutR extends Turnout {
	@Override
	public Object click() throws IOException {
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else {
			state = (state == State.STRAIGHT) ? State.RIGHT : State.STRAIGHT;
			plan.stream("place "+tag(null));
		}
		return propMenu();
	}
}
