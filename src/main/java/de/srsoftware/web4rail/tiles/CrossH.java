package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;

public class CrossH extends Cross{
	
	@Override
	public List<Connector> connections(String from) {
		switch (from) {
			case Plan.NORTH:
				return List.of(new Connector(x+1,y+1,Plan.NORTH));
			case Plan.SOUTH:
				return List.of(new Connector(x+1,y-1,Plan.SOUTH));
		}
		return new Vector<>();
	}
	
	@Override
	public int len() {
		return 2;
	}
	
	@Override
	public List<Connector> offsetConnections(String from) {
		switch (from) {
		case Plan.NORTH:
			return List.of(new Connector(x,y+1,Plan.NORTH));
		case Plan.SOUTH:
			return List.of(new Connector(x,y-1,Plan.SOUTH));
	}
	return new Vector<>();
}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(200,100).attr("viewbox", "0 0 200 100");
	}
}
