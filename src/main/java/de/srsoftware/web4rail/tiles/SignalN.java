package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class SignalN extends Signal {
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (oneWay == from) return new HashMap<>();
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x,y+1,Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public boolean isAffectedFrom(Direction dir) {
		return dir == Direction.NORTH;
	}
	
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.NORTH,Direction.SOUTH);
	}
}
