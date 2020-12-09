package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
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

	public Shadow(TileWithShadow overlay, int x, int y) {		
		this.overlay = overlay;
		this.x = x;
		this.y = y;
		overlay.add(this);
	}

	public Tile overlay() {
		return overlay;
	}
	
	@Override
	public void removeChild(BaseClass child) {
		super.removeChild(child);
		if (child == overlay) {
			overlay = null;
			remove();
		}		
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		if (overlay instanceof Block) tag.attr("class", tag.get("class")+" Block");
		return tag;
	}
}
