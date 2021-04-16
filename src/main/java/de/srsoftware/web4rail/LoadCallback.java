package de.srsoftware.web4rail;

import java.util.LinkedList;

public abstract class LoadCallback {
	
	private static LinkedList<LoadCallback> callbacks = new LinkedList<LoadCallback>();
	
	public LoadCallback() {
		callbacks.add(this);
	}
	
	public abstract void afterLoad();
	
	public static void fire() {
		while (!callbacks.isEmpty()) callbacks.removeFirst().afterLoad();
	}
}
