package de.srsoftware.web4rail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

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
		column.put(y,tile.position(x, y));
		return old;
	}
	
	public Tile get(int x, int y) {
		HashMap<Integer, Tile> column = tiles.get(x);
		return column == null ? null : column.get(y);
	}

	public Page html() throws IOException {
		Page page = new Page();
		for (Entry<Integer, HashMap<Integer, Tile>> column : tiles.entrySet()) {
			int x = column.getKey();
			for (Entry<Integer, Tile> row : column.getValue().entrySet()) {
				int y = row.getKey();
				Tile tile = row.getValue().position(x, y);
				page.append("\t\t"+tile.html()+"\n");
			}
		}
		return page;
	}
}
