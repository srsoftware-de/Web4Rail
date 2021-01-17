package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

/**
 * @author Stephan Richter, SRSoftware 2020-2021 * 
 */
public class Window extends Tag{

	private static final long serialVersionUID = 9035075889261889575L;

	public Window(String id, String title) {
		super("div");
		id(id).clazz("window");
		new Tag("h2")
			.clazz("title")
			.content(title).addTo(this);
		new Tag("div")
			.clazz("closebtn")
			.attr("onclick", "return closeWindows();")
			.content("&times;").addTo(this);		
		new Tag("div")
		.clazz("swapbtn")
		.attr("onclick", "return swapTiling();")
		.content("◧").addTo(this);		
	}
}
