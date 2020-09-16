package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class CrossV extends Cross{
	
	@Override
	public int height() {
		return 2;
	}
	
	@Override
	public Map<Connector, State> offsetConnections(Direction from) {
		return null;
	}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(100,200).attr("viewbox", "0 0 100 200");
	}
}
