package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;

public class CrossV extends Cross{
	
	@Override
	public int height() {
		return 2;
	}
	
	@Override
	public List<Connector> offsetConnections(String from) {
		return null;
	}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(100,200).attr("viewbox", "0 0 100 200");
	}
}
