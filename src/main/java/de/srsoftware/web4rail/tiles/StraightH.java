package de.srsoftware.web4rail.tiles;

import java.io.IOException;

import de.srsoftware.tools.Tag;

public class StraightH extends StretchableTile{
	
	@Override
	public int len() {
		return length;
	}
	
	@Override
	public Tag tag() throws IOException {
		Tag tag = super.tag();
		if (length>1) {
			String style = tag.get("style");
			tag.style(style.trim()+" width: "+(30*length)+"px;");
			tag.attr("preserveAspectRatio","none");
		}
		return tag;
	}
}
