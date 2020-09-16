package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class ContactH extends Contact {
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case WEST:
				return List.of(new Connector(x+1,y,from));
			case EAST:
				return List.of(new Connector(x-1,y,from));
			default:
				return new Vector<>();
		}		
	}
}
