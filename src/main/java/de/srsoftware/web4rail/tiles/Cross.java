package de.srsoftware.web4rail.tiles;

import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public abstract class Cross extends Tile {
	public abstract Map<Connector,Turnout.State> offsetConnections(Direction from);
}
