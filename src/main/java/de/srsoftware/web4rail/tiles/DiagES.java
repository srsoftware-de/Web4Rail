package de.srsoftware.web4rail.tiles;

public class DiagES extends Tile{

	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case SOUTH:
		case EAST:
			return true;
		default:
			return false;
		}
	}	

}
