package de.srsoftware.web4rail;

import java.util.List;
import java.util.Vector;

import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;

public class Route {
	
	private Vector<Tile> path;
	private Vector<Signal> signals;
	private String id;
	private String name;

	public Tile add(Tile tile) {
		path.add(tile);
		return tile;
	}
	
	protected Route clone() {
		Route clone = new Route();
		clone.path = new Vector<>(path);
		return clone;
	}
	
	public String id() {
		if (id == null) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<path.size();i++) {
				Tile tile = path.get(i);
				if (i==0) {
					sb.append(((Block)tile).name);
				} else if (i==path.size()-1){
					sb.append("-"+((Block)tile).name);
				} else {
					sb.append("-"+tile.x+":"+tile.y);
				}
			}
			id = sb.toString();
		}
		return id;
	}

	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}
	
	public String name() {
		if (name == null) name = id();
		return name;
	}

	public Route start(Block block) {
		path = new Vector<Tile>();
		path.add(block);
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+id()+")";
	}

	public Block start() {
		return (Block) path.get(0);
	}
}
