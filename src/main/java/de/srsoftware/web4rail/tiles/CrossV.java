package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;

public class CrossV extends Tile{
	
	@Override
	public int height() {
		return 2;
	}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(100,200).attr("viewbox", "0 0 100 200");
	}
}
