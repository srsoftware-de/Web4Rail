package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class SignalE extends Signal{
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.WEST:
				return List.of(new Connector(x+1,y,from));
			case Plan.EAST:
				return List.of(new Connector(x-1,y,from));
		}
		return new Vector<>();
	}
}
