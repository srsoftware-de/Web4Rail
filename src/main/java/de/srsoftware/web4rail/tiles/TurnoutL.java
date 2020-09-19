package de.srsoftware.web4rail.tiles;

import java.io.IOException;

public class TurnoutL extends Turnout {
	@Override
	public Object click() throws IOException {
		if (lockedBy != null) return t("{} is locked by {}!",this,lockedBy); 
		state = (state == State.STRAIGHT) ? State.LEFT : State.STRAIGHT;
		return tag(null);
	}
}
