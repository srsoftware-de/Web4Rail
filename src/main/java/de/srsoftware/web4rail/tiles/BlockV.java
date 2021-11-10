package de.srsoftware.web4rail.tiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class BlockV extends Block{
	Contact west,center,east;
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from)) return new HashMap<Connector, Turnout.State>();
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x,y+height(),Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public Direction determineDirection(String id) {
		Set<Direction> endDirections = arrivingRoutes().stream().map(Route::endDirection).collect(Collectors.toSet());
		if (endDirections.size()<2) return endDirections.stream().findAny().get();
		if (stretch()<2) return null;
		if (id().equals(id)) return directionB();
		if ((x+"-"+(y+stretch()-1)).equals(id)) return directionA();
		return null; 
	}
	
	@Override
	public Direction directionA() {
		return Direction.NORTH;
	}
		
	@Override
	public Direction directionB() {
		return Direction.SOUTH;
	}
	
	@Override
	public int height() {
		return stretch();
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x,y-1,Direction.SOUTH),new Connector(x,y+height(),Direction.NORTH));
	}

	@Override
	protected String stretchType() {
		return t("Height");
	}	
}
