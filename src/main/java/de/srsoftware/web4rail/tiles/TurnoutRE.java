package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class TurnoutRE extends TurnoutR{

	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case EAST:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.RIGHT,new Connector(x-1, y, Direction.EAST),State.STRAIGHT);
			case NORTH:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.RIGHT);
			case WEST:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.STRAIGHT);
			default:
				return new HashMap<>();
		}
	}
}
