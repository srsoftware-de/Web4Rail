package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class ContactV extends Contact {
	
	@Override
	public List<Connector> connections(Direction from) {
		switch (from) {
			case NORTH:
				return List.of(new Connector(x,y+1,from));
			case SOUTH:
				return List.of(new Connector(x,y-1,from));
			default:
				return new Vector<>();
		}		
	}
}
