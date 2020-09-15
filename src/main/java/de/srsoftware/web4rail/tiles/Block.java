package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Set;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Route;

public abstract class Block extends StretchableTile{
	public abstract Set<Route> routes();

	public abstract List<Connector> startPoints();
}
