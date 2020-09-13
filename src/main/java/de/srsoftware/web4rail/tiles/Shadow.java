package de.srsoftware.web4rail.tiles;

public class Shadow extends Tile{

	private Tile overlay;

	public Shadow(Tile overlay) {
		this.overlay = overlay;
	}

	public Tile overlay() {
		return overlay;
	}
}
