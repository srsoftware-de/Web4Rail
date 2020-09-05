package de.srsoftware.web4rail.tiles;

public class TurnoutSE extends Turnout{

	@Override
	public boolean hasConnector(Direction direction) {
		return direction != Direction.WEST;
	}

}
