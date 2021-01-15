package de.srsoftware.web4rail;

import de.srsoftware.tools.Tag;

public interface Device {
	public static final String ADDRESS = "address";
	public static final String PROTOCOL = "proto";
	
	public int address();

	public Tag link(String...args);
}
