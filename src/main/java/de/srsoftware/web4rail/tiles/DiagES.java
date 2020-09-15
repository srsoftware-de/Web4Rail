package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class DiagES extends Tile{

	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.SOUTH:
				return List.of(new Connector(x+1,y,Plan.WEST));
			case Plan.EAST:
				return List.of(new Connector(x,y+1,Plan.NORTH));
		}
		return new Vector<>();
	}
}
