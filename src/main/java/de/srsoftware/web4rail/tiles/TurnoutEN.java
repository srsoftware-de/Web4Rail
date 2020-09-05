package de.srsoftware.web4rail.tiles;

public class TurnoutEN extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.SOUTH;
	}

}
