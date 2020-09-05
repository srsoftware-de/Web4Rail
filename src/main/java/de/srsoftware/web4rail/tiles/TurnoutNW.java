package de.srsoftware.web4rail.tiles;

public class TurnoutNW extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.EAST;
	}

}
