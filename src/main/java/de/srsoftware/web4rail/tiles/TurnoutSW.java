package de.srsoftware.web4rail.tiles;

public class TurnoutSW extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.EAST;
	}
	
}
