package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class TagWithId extends Tag {

	public TagWithId(String type, String id) {
		super(type);
		id(id);
	}

	private static final long serialVersionUID = 6349230653857414636L;	
}
