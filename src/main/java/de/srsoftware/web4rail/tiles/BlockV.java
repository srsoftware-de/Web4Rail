package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class BlockV extends Block{
	Contact west,center,east;
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x,y+height(),Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public int height() {
		return length;
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x,y-1,Direction.SOUTH),new Connector(x,y+height(),Direction.NORTH));
	}	
}
