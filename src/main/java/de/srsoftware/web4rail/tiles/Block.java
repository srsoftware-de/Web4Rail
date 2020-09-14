package de.srsoftware.web4rail.tiles;

import java.util.Set;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tiles.StretchableTile;

public abstract class Block extends StretchableTile{
	public abstract Set<Route> routes();
}
