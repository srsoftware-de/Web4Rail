package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class Shadow extends Tile{

	private Tile overlay;
	
	@Override
	public List<Connector> connections(Direction from) {
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
}
