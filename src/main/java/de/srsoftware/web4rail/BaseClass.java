package de.srsoftware.web4rail;

public class BaseClass implements Constants{
	public static boolean isNull(Object o) {
		return o==null;
	}

	public static boolean isSet(Object o) {
		return o != null;
	}
}
