package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class StraightH extends StretchableTile{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.WEST:
				return List.of(new Connector(x+len(),y,from));
			case Plan.EAST:
				return List.of(new Connector(x-1,y,from));
		}
		return new Vector<>();
	}
	
	@Override
	public int len() {
		return length;
	}
}
