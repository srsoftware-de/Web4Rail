package de.srsoftware.web4rail.tiles;

import java.io.IOException;

public class TurnoutR extends Turnout {
	@Override
	public Object click() throws IOException {
		state = (state == State.STRAIGHT) ? State.RIGHT : State.STRAIGHT;
		return tag(null);
	}
}
