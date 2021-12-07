package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;

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
	
	public void highlight(BaseClass element) {
		BaseClass scrollTarget = element.parent();
		if (scrollTarget == null) scrollTarget = element;
		
		children().add(new Tag("script").content("document.getElementById('"+scrollTarget.id()+"').scrollIntoView({ behavior: \"smooth\" }); document.getElementById('"+element.id()+"').classList.add('highlight');"));
	}
}
