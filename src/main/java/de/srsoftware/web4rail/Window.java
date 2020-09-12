package de.srsoftware.web4rail;

import de.srsoftware.tools.Tag;

public class Window extends Tag{

	private static final long serialVersionUID = 9035075889261889575L;

	public Window(String id) {
		super("div");
		id(id);
		new Tag("div").clazz("closebtn").attr("onclick", "$('#"+id+"').remove(); return false").content("&times;").addTo(this);
	}
}
