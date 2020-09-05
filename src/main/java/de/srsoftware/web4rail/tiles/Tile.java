package de.srsoftware.web4rail.tiles;

import java.util.HashMap;

public abstract class Tile {
	
	public enum Direction{
		NORTH,SOUTH,EAST,WEST;
	}
	private class Position {
		int x,y;
	}
	
	Position position;
	private HashMap<Direction,Tile> neighbours = new HashMap();
	
	public abstract boolean hasConnector(Direction direction);
	
	public boolean connect(Direction direction, Tile neighbour) {
		if (hasConnector(direction)) {
			switch (direction) {
				case NORTH:
					neighbour.neighbours.put(Direction.SOUTH, this);
					neighbour.position.x = position.x;
					neighbour.position.y = position.y-1;
				case SOUTH:
					neighbour.neighbours.put(Direction.NORTH, this);
					neighbour.position.x = position.x;
					neighbour.position.y = position.y+1;
				case EAST:
					neighbour.neighbours.put(Direction.WEST, this);
					neighbour.position.x = position.x+1;
					neighbour.position.y = position.y;
				case WEST:
					neighbour.neighbours.put(Direction.EAST, this);
					neighbour.position.x = position.x-1;
					neighbour.position.y = position.y;					
			}
			neighbours.put(direction, neighbour);
			return true;
		}
		return false;
	}
	
	public Tile neighbour(Direction direction) {
		return neighbours.get(direction);
	}

	public Tile position(int x, int y) {
		position.x = x;
		position.y = y;
		return this;
	}
	
}
