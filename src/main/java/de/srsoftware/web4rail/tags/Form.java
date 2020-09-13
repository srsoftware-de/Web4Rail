package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Form extends Tag {

	private static final long serialVersionUID = 3518580733330482303L;

	public Form() {
		super("form");
		attr("method","POST");
	}

}
