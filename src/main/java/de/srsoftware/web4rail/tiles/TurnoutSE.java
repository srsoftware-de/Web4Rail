package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class TurnoutSE extends Turnout{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.SOUTH:
				return List.of(new Connector(x+1,y,Plan.WEST),new Connector(x, y-1, Plan.SOUTH));
			case Plan.EAST:
			case Plan.NORTH:
				return List.of(new Connector(x,y+1,Plan.NORTH));
		}
		return new Vector<>();
	}
}
