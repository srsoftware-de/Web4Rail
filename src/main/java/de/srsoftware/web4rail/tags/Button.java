package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Button extends Tag {

	private static final long serialVersionUID = -7785030725633284515L;

	public Button(String text) {
		super("button");
		attr("type", "submit");
		content(text);
	}
	
	public Button(String text,String action) {
		super("button");
		attr("onclick",action).content(text);
	}

}
