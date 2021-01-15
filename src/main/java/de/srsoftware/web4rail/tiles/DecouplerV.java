package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class DecouplerV extends Decoupler{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x+width(),y,Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x-1,y,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.SOUTH,Direction.NORTH);
	}
}
