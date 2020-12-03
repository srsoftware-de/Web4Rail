package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class StraightH extends StretchableTile{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from) || oneWay == from) return new HashMap<>();
		switch (from) {
			case WEST:
				return Map.of(new Connector(x+width(),y,Direction.WEST),State.UNDEF);
			case EAST:
				return Map.of(new Connector(x-1,y,Direction.EAST),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public List<Direction> possibleDirections() {
		return List.of(Direction.EAST,Direction.WEST);
	}

	@Override
	protected String stretchType() {
		return t("Width");
	}
	
	@Override
	public int width() {
		return stretch;
	}
}
