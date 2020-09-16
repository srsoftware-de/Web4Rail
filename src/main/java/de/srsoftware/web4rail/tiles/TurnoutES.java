package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class TurnoutES extends Turnout{
	
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case EAST:
				return List.of(new Connector(x,y+1,Direction.NORTH),new Connector(x-1, y, Direction.EAST));
			case SOUTH:
			case WEST:
				return List.of(new Connector(x+1,y,Direction.WEST));			
			default:
				return new Vector<>();
		}
	}
}
