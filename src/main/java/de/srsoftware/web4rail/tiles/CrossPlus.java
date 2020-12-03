package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class CrossPlus extends Tile{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case WEST:
				return Map.of(new Connector(x+width(),y,Direction.WEST),State.UNDEF);
			case EAST:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.UNDEF);
			case NORTH:
				return Map.of(new Connector(x,y+height(),Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.EAST,Direction.WEST,Direction.NORTH,Direction.SOUTH);
	}	
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
