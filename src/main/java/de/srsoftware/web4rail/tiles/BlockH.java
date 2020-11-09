package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class BlockH extends Block{
	Contact north,center,south;
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from)) return new HashMap<Connector, Turnout.State>();
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
	public Direction directionA() {
		return Direction.WEST;
	}
		
	@Override
	public Direction directionB() {
		return Direction.EAST;
	}
		
	@Override
	public int width() {
		return stretch;
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x-1, y, Direction.EAST),new Connector(x+width(), y, Direction.WEST));
	}

	@Override
	protected String stretchType() {
		return t("Width");
	}
}
