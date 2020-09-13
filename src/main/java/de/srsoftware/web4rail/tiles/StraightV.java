package de.srsoftware.web4rail.tiles;

import java.io.IOException;

import de.srsoftware.tools.Tag;

public class StraightV extends StretchableTile{
	
	@Override
	public int height() {
		return length;
	}
	
	@Override
	public Tag tag() throws IOException {
		Tag tag = super.tag();
		if (length>1) {
			LOG.debug("{}.tag: length = {}",getClass().getSimpleName(),length);
			String style = tag.get("style");
			tag.style(style.trim()+" height: "+(30*length)+"px;");
			tag.attr("preserveAspectRatio","none");
		}
		return tag;
	}
}
