package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class DiagWN extends Tile{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.NORTH:
				return List.of(new Connector(x-1,y,Plan.EAST));
			case Plan.WEST:
				return List.of(new Connector(x,y-1,Plan.SOUTH));
		}
		return new Vector<>();
	}
}
