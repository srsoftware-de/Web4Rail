package de.srsoftware.web4rail.tiles;

public class DiagNE extends Tile{

	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case NORTH:
		case EAST:
			return true;
		default:
			return false;
		}
	}	

}
