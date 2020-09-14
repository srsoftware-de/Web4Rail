package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;

public class CrossH extends Tile{
	
	@Override
	public int len() {
		return 2;
	}
	
	@Override
	public Tag tag(Map<String,Object> replacements) throws IOException {
		return super.tag(replacements).size(200,100).attr("viewbox", "0 0 200 100");
	}
}
