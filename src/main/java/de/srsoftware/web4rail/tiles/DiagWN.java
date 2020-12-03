package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class DiagWN extends Tile{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from)) return new HashMap<>();

		switch (from) {
			case NORTH:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.UNDEF);
			case WEST:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
}
