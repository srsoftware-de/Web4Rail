package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class StraightH extends StretchableTile{
	
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case WEST:
				return List.of(new Connector(x+len(),y,Direction.WEST));
			case EAST:
				return List.of(new Connector(x-1,y,Direction.EAST));
			default:
				return new Vector<>();
		}
	}
	
	@Override
	public int len() {
		return length;
	}
}
