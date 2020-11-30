package de.srsoftware.web4rail.tiles;

import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class BridgeW extends Bridge {
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.WEST);
	}

	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isSet(counterpart))
			switch (from) {
			case WEST:
				return Map.of(counterpart.connector(), State.UNDEF);
			default:
		}
		return super.connections(from);
	}

	@Override
	protected Connector connector() {
		return new Connector(x - 1, y, Direction.EAST);
	}
}
