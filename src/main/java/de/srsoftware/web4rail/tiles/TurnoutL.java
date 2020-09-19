package de.srsoftware.web4rail.tiles;

import java.io.IOException;

public class TurnoutL extends Turnout {
	@Override
	public Object click() throws IOException {
		if (lockedBy != null) {
			plan.stream(t("{} is locked by {}!",this,lockedBy)); 
		} else {
			state = (state == State.STRAIGHT) ? State.LEFT : State.STRAIGHT;
			plan.stream("place "+tag(null));
		}
		return propMenu();
	}
}
