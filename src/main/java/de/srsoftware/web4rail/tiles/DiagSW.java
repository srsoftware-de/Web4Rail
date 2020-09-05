package de.srsoftware.web4rail.tiles;

public class DiagSW extends Tile{

	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case SOUTH:
		case WEST:
			return true;
		default:
			return false;
		}
	}	

}
