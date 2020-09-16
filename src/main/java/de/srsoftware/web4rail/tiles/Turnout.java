package de.srsoftware.web4rail.tiles;

public abstract class Turnout extends Tile {
	public static final String STATE = "state";
	public enum State{
		LEFT,STRAIGHT,RIGHT,UNDEF;
	}
	private boolean straight = true;
	
	public boolean toggle() {
		straight = !straight;
		return straight;
	}
}
