package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Range extends Tag{

	private static final long serialVersionUID = 1865176096163142641L;
	private Tag range;
	private Tag caption;

	public Range(String caption, String name, int current, int min, int max) {
		super("div");
		this.caption = new Tag("span").content(caption);
		this.caption.addTo(this);
		Tag label = new Tag("label").content(""+min);
		range = new Input(name).attr("type","range").attr("min",min).attr("max",max).attr("value",current);
		range.addTo(label);		
		label.content(""+max);
		label.addTo(this);		
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Range id(String id) {
		range.id(id);
		caption.id(id+"_caption");
		return this;
	}
	
	public Range onChange(String script) {
		range.attr("onchange", script);
		return this;
	}
}
