package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Select extends Tag {
	private static final long serialVersionUID = -2168654457876014503L;

	public Select(String name) {
		super("select");
		attr("name",name);
	}

	public Tag addOption(Object value, Object text) {
		Tag option = new Tag("option").attr("value", value.toString()).content(text.toString());
		option.addTo(this);
		return option;
	}
}
