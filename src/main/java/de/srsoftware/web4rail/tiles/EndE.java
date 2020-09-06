package de.srsoftware.web4rail.tiles;

import de.srsoftware.tools.Tag;

public class EndE extends Tile{
	public String html() {
		Tag svg = new Tag("svg")
				.id("tile-"+x+"-"+y)
				.clazz(classes)
				.size(100,100)
				.attr("viewbox", "0 0 100 100")
				.style("left: "+(30*x)+"px; top: "+(30*y)+"px");

		new Tag("rect")
				.size(60,30)
				.pos(40,35)
				.addTo(svg);
		
		return svg.toString();
	}

}
