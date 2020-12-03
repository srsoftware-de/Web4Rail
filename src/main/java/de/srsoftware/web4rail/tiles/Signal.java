package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;

public abstract class Signal extends Tile {
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
	
	public abstract boolean isAffectedFrom(Direction dir);

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
