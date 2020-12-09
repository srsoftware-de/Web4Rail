package de.srsoftware.web4rail.tiles;

import java.util.Vector;

import de.srsoftware.web4rail.BaseClass;

public class TileWithShadow extends Tile {
	private Vector<Id> shadows = new Vector<Id>();
	
	public void add(Shadow shadow) {
		shadows.add(shadow.id());
	}
	
	@Override
	public boolean move(int dx, int dy) {
		boolean moved = super.move(dx, dy);
		if (moved) placeShadows();
		return moved;
	}

	public void placeShadows() {
		removeShadows();
		for (int dx=1; dx<width(); dx++) plan.place(new Shadow(this, x+dx, y));
		for (int dy=1; dy<height(); dy++) plan.place(new Shadow(this, x, y+dy));
	}	
	
	protected void removeShadows() {
		while (!shadows.isEmpty()) {
			Tile tile = BaseClass.get(shadows.remove(0));
			if (tile instanceof Shadow) tile.remove();
		}
	}
}
