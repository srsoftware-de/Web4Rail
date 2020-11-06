package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class TurnoutRN extends TurnoutR{

	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.RIGHT,new Connector(x, y+1, Direction.NORTH),State.STRAIGHT);
			case WEST:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.RIGHT);
			case SOUTH:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.STRAIGHT);
			default:
				return new HashMap<>();
		}
	}
}
