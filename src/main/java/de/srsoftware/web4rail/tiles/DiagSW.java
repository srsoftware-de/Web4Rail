package de.srsoftware.web4rail.tiles;

import de.srsoftware.tools.Tag;

public class DiagSW extends Tile{
	public String html() {
		Tag svg = new Tag("svg")
				.id("tile-"+x+"-"+y)
				.clazz(classes)
				.size(100,100)
				.attr("viewbox", "0 0 100 100")
				.style("left: "+(30*x)+"px; top: "+(30*y)+"px");

		new Tag("polygon")
			.attr("points","0,35 65,100 35,100 0,65")
			.addTo(svg);
		
		return svg.toString();
	}
}
