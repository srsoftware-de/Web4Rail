package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Input extends Tag{

	private static final long serialVersionUID = -330127933233033028L;
	public static final String NAME = "name";
	public static final String VALUE = "value";

	public Input(String name) {
		super("input");
		attr("type","text").attr("name", name);
	}	

	public Input(String name, Object value) {
		super("input");
		attr("type","text").attr(NAME, name).attr(VALUE, value.toString());
	}

	public Tag hideIn(Tag form) {
		return this.attr("type", "hidden").addTo(form);
	}
	
	public Input numeric() {
		attr("type","number");
		return this;
	}
}
