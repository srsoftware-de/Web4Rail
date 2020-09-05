package de.srsoftware.web4rail.tiles;

public class TurnoutNE extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.WEST;
	}
	
}
