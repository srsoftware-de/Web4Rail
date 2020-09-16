package de.srsoftware.web4rail;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;

public class Route {
	
	private Vector<Tile> path;
	private Vector<Signal> signals;

	public Tile add(Tile tile) {
		path.add(tile);
		return tile;
	}
	
	protected Route clone() {
		Route clone = new Route();
		clone.path = new Vector<>(path);
		return clone;
	}

	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}

	public Route start(Block block) {
		path = new Vector<Tile>();
		path.add(block);
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+path+")";
	}
}
