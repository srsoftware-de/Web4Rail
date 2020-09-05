package de.srsoftware.web4rail.tiles;

public class StraightH extends StretchableTile{
	@Override
	public boolean hasConnector(Direction direction) {
		switch (direction) {
		case EAST:
		case WEST:
			return true;
		default:
			return false;
		}
	}	
}
