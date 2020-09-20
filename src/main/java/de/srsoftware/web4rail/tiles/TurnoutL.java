package de.srsoftware.web4rail.tiles;

import java.io.IOException;

public class TurnoutL extends Turnout {
	@Override
	public Object click() throws IOException {
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else {
			state = (state == State.STRAIGHT) ? State.LEFT : State.STRAIGHT;
			plan.stream("place "+tag(null));
		}
		return propMenu();
	}
}
