package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class TurnoutLW extends TurnoutL{

	@Override
	public Map<Connector, State> connections(Direction from) {
		switch (from) {
			case WEST:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.STRAIGHT,new Connector(x, y-1, Direction.SOUTH),State.LEFT);
			case EAST:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.STRAIGHT);
			case NORTH:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.LEFT);
			default:
				return new HashMap<>();
		}
	}
}
