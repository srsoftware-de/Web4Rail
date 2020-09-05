package de.srsoftware.web4rail;

import java.util.HashMap;

import de.srsoftware.web4rail.tiles.Tile;

public class Plan {
	private HashMap<Integer,HashMap<Integer,Tile>> tiles = new HashMap<Integer,HashMap<Integer,Tile>>();
	
	public Tile set(int x,int y,Tile tile) {
		Tile old = null;
		HashMap<Integer, Tile> column = tiles.get(x);
		if (column == null) {
			column = new HashMap<Integer,Tile>();
			tiles.put(x, column);
		}
		old = column.get(y);
		column.put(y,tile);
		return old;
	}
	
	public Tile get(int x, int y) {
		HashMap<Integer, Tile> column = tiles.get(x);
		return column == null ? null : column.get(y).position(x,y);
	}
}
