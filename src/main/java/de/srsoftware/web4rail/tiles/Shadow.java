package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;

public class Shadow extends Tile{

	private Tile overlay;
	
	@Override
	public List<Connector> connections(String from) {
		if (overlay instanceof StretchableTile) return overlay.connections(from);
		if (overlay instanceof Cross) return ((Cross)overlay).offsetConnections(from);
		return super.connections(from);
	}

	public Shadow(Tile overlay) {
		this.overlay = overlay;
	}

	public Tile overlay() {
		return overlay;
	}
}
