package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class BlockV extends Block{
	Contact west,center,east;
	
	@Override
	public int height() {
		return length;
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x,y-1,Direction.SOUTH),new Connector(x,y+height(),Direction.NORTH));
	}	
}
