package de.srsoftware.web4rail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tiles.DiagES;
import de.srsoftware.web4rail.tiles.DiagNE;
import de.srsoftware.web4rail.tiles.DiagSW;
import de.srsoftware.web4rail.tiles.DiagWN;
import de.srsoftware.web4rail.tiles.EndE;
import de.srsoftware.web4rail.tiles.EndW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.TurnoutSE;
import de.srsoftware.web4rail.tiles.TurnoutSW;
import de.srsoftware.web4rail.tiles.TurnoutWS;

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
		page.append(menu());
		return page;
	}

	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		Tag tileMenu = new Tag("div").clazz("tile").content("Add tile");
		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new StraightH().html());
		tiles.append(new StraightV().html());
		tiles.append(new DiagES().html());
		tiles.append(new DiagSW().html());
		tiles.append(new DiagNE().html());
		tiles.append(new DiagWN().html());
		tiles.append(new EndE().html());
		tiles.append(new EndW().html());
		tiles.append(new TurnoutSE().html());
		tiles.append(new TurnoutWS().html());
		tiles.append(new TurnoutSW().html());
		new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu).addTo(menu);
			
		return menu;
	}
}
