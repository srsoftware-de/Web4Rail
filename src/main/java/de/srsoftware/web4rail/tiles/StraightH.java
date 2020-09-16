package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class StraightH extends StretchableTile{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		switch (from) {
			case WEST:
				return Map.of(new Connector(x+len(),y,Direction.WEST),State.UNDEF);
			case EAST:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public int len() {
		return length;
	}
}
