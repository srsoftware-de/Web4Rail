package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Plan.Direction;

public abstract class Signal extends Tile implements Comparable<Signal>{
	public static final String STATE = "state";
	public static final String STOP = "stop";
	public static final String GO = "go";
	
	public static final TreeSet<String> knownStates = new TreeSet<String>(List.of(STOP, GO));
	
	private String state = STOP;

	public Signal() {
		super();
	}
	
	@Override
	protected Vector<String> classes() {
		Vector<String> classes = super.classes();
		classes.add("signal");
		return classes;
	}
	
	@Override
	public int compareTo(Signal other) {
		Id tid = this.id();
		Id oid = other.id();
		return tid.compareTo(oid);
	}
	
	public abstract boolean isAffectedFrom(Direction dir);
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}

	public boolean state(String state) {
		this.state = state;
		plan.place(this);
		return true;
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+" "+state);
		return tag;
	}
}
