package de.srsoftware.web4rail.tiles;

public class TurnoutWS extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.NORTH;
	}

}
