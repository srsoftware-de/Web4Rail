package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class ContactH extends Contact {
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case WEST:
				return Map.of(new Connector(x+1,y,from),State.UNDEF);
			case EAST:
				return Map.of(new Connector(x-1,y,from),State.UNDEF);
			default:
				return new HashMap<>();
		}		
	}
	
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.EAST,Direction.WEST);
	}
}
