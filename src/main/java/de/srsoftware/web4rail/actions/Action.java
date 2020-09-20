package de.srsoftware.web4rail.actions;

import java.io.IOException;

public abstract class Action {
	public abstract void fire() throws IOException;
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
