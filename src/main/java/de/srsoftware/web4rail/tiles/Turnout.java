package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;

public abstract class Turnout extends Tile {
	public static final String STATE = "state";
	public enum State{
		LEFT,STRAIGHT,RIGHT,UNDEF;
	}
	protected State state = State.STRAIGHT;
	
	public State state() {
		return state;
	}
	
	public void state(State newState) throws IOException {
		state = newState;
		LOG.debug("Setting {} to {}",this,state);
		plan.stream("place "+tag(null));
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+(" "+state).toLowerCase());
		return tag;
	}
	
	public void toggle() {
		state = State.STRAIGHT;
	}
}
