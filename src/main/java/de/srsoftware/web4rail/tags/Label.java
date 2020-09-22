package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Label extends Tag {

	private static final long serialVersionUID = -2483427530977586755L;
	
	public Label(String label) {
		super("label");
		content(label);
	}
}
