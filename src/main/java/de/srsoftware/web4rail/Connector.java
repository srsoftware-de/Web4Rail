package de.srsoftware.web4rail;

public class Connector {

	private Plan.Direction from;
	private int y;
	private int x;

	public Connector(int x, int y, Plan.Direction from) {
		this.x = x;
		this.y = y;
		this.from = from;
	}
	
	public Plan.Direction from() {
		return from;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+x+", "+y+", from "+from+")";
	}
}
