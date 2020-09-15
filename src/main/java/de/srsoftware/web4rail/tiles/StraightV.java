package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class StraightV extends StretchableTile{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.NORTH:
				return List.of(new Connector(x,y+height(),from));
			case Plan.SOUTH:
				return List.of(new Connector(x,y-1,from));
		}
		return new Vector<>();
	}
	
	@Override
	public int height() {
		return length;
	}
}
