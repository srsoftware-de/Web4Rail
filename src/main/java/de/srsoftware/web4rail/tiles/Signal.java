package de.srsoftware.web4rail.tiles;

import de.srsoftware.web4rail.Plan.Direction;

public abstract class Signal extends Tile{

	public Signal() {
		super();
		classes.add("signal");
	}
	
	public abstract boolean isAffectedFrom(Direction dir);
}
