package de.srsoftware.web4rail;

/**
 * @author Stephan Richter, SRSoftware 2020…2021 
 */
public class Connector {

	public Plan.Direction from;
	public int y;
	public int x;

	public Connector(int x, int y, Plan.Direction from) {
		this.x = x;
		this.y = y;
		this.from = from;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+x+", "+y+", from "+from+")";
	}
}
