package de.srsoftware.web4rail.tiles;

public abstract class Turnout extends Tile {

	private boolean straight = true;
	
	public boolean toggle() {
		straight = !straight;
		return straight;
	}
}
