package de.srsoftware.web4rail.tiles;

public class DiagWN extends Tile{

	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case NORTH:
		case WEST:
			return true;
		default:
			return false;
		}
	}	

}
