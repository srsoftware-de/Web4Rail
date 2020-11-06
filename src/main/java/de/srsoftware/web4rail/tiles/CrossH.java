package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class CrossH extends Cross{
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (isNull(from)) return new HashMap<>();
		switch (from) {
			case NORTH:
				return Map.of(new Connector(x+1,y+1,Direction.NORTH),State.UNDEF);
			case SOUTH:
				return Map.of(new Connector(x+1,y-1,Direction.SOUTH),State.UNDEF);
			default:
				return new HashMap<>();
		}
	}
	
	@Override
	public int width() {
		return 2;
	}
	
	@Override
	public Map<Connector,State> offsetConnections(Direction from) {
		if (isNull(from)) return new HashMap<>();
		switch (from) {
		case NORTH:
			return Map.of(new Connector(x,y+1,Direction.NORTH),State.UNDEF);
		case SOUTH:
			return Map.of(new Connector(x,y-1,Direction.SOUTH),State.UNDEF);
		default:
			return new HashMap<>();
	}
	
}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(200,100).attr("viewbox", "0 0 200 100");
	}
}
