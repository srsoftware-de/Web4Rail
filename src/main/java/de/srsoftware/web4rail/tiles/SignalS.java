package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class SignalS extends Signal{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
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
		return dir == Direction.SOUTH;
	}
}
