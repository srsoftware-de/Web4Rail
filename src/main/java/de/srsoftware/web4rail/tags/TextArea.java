package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class TextArea extends Tag{

	private static final long serialVersionUID = -330127933233033028L;
	public static final String NAME = "name";
	public static final String VALUE = "value";

	public TextArea(String name) {
		this(name,"");
	}	

	public TextArea(String name, Object value) {
		super("textarea");
		attr("name", name);
		content(value == null?"":value.toString());
	}

	public Tag hideIn(Tag form) {
		return this.attr("type", "hidden").addTo(form);
	}
	
	public TextArea numeric() {
		attr("type","number");
		return this;
	}
}
