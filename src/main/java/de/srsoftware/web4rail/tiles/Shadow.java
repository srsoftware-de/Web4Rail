package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class Shadow extends Tile{

	private Tile overlay;
	
	@Override
	public Map<Connector, State> connections(Direction from) {
		if (overlay instanceof StretchableTile) return overlay.connections(from);
		if (overlay instanceof Cross) return ((Cross)overlay).offsetConnections(from);
		return super.connections(from);
	}

	public Shadow(Tile overlay) {		
		this.overlay = overlay;
		overlay.addShadow(this);
	}

	public Tile overlay() {
		return overlay;
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		if (overlay instanceof Block) tag.attr("class", tag.get("class")+" Block");
		return tag;
	}
}
