package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.ControlUnit.Reply;
import de.srsoftware.web4rail.Plan.Direction;

public class Turnout3E extends Turnout{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		switch (from) {
			case EAST:
				return Map.of(
						new Connector(x,y+1,Direction.NORTH),State.LEFT,
						new Connector(x-1, y, Direction.EAST),State.STRAIGHT,
						new Connector(x,y-1,Direction.SOUTH),State.RIGHT);
			case NORTH:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.RIGHT);
			case SOUTH:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.LEFT);
			case WEST:
				return Map.of(new Connector(x+1,y,Direction.WEST),State.STRAIGHT);			
			default:
				return new HashMap<>();
		}
	}

	@Override
	public CompletableFuture<Reply> state(State newState) throws IOException {
		// TODO Auto-generated method stub
		LOG.warn("Turnout3E.state({}) not implemented, yet!",newState);
		return null;
	}
}
