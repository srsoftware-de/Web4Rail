package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;

public abstract class Cross extends Tile {
	public abstract List<Connector> offsetConnections(String from);
}
