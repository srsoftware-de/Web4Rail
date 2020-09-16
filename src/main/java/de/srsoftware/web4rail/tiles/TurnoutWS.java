package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class TurnoutWS extends Turnout{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.WEST:
				return List.of(new Connector(x+1,y,Plan.WEST),new Connector(x, y+1, Plan.NORTH));
			case Plan.EAST:
			case Plan.SOUTH:
				return List.of(new Connector(x-1,y,Plan.EAST));
		}
		return new Vector<>();
	}
}
