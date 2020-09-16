package de.srsoftware.web4rail.tiles;

import java.util.List;

import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;

public class BlockH extends Block{
	Contact north,center,south;
		
	@Override
	public int len() {
		return length;
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x-1, y, Direction.EAST),new Connector(x+len(), y, Direction.WEST));
	}
}
