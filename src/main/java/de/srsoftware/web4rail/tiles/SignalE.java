package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class SignalE extends Signal{
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case WEST:
				return List.of(new Connector(x+1,y,Direction.WEST));
			case EAST:
				return List.of(new Connector(x-1,y,Direction.EAST));
			default:
				return new Vector<>();
		}
	}
	
	@Override
	boolean isAffectedFrom(Direction dir) {
		return dir == Direction.EAST;
	}
}
