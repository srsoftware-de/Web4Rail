package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.Map;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class DiagES extends Tile{

	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from)) return new HashMap<>();
		switch (from) {
			case SOUTH:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.UNDEF);
			case EAST:
				return Map.of(new Connector(x,y+1,Direction.NORTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
