package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public abstract class Cross extends Tile {
	public abstract List<Connector> offsetConnections(Direction from);
}
