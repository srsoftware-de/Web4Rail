package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class DiagNE extends Tile{
	
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case NORTH:
				return List.of(new Connector(x+1,y,Direction.WEST));
			case EAST:
				return List.of(new Connector(x,y-1,Direction.SOUTH));
			default:
				return new Vector<>();
		}
	}
}
