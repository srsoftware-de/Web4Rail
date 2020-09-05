package de.srsoftware.web4rail.tiles;

public class StraightV extends StretchableTile{

	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case NORTH:
		case SOUTH:
			return true;
		default:
			return false;
		}
	}	
}
