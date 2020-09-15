package de.srsoftware.web4rail;

public class Connector {

	private String from;
	private int y;
	private int x;

	public Connector(int x, int y, String from) {
		this.x = x;
		this.y = y;
		this.from = from;
	}
	
	public String from() {
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
