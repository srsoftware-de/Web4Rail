package de.srsoftware.web4rail.tiles;

import de.srsoftware.tools.Tag;

public class TurnoutSE extends Turnout{
	public String html() {
		Tag svg = new Tag("svg")
				.id("tile-"+x+"-"+y)
				.clazz(classes)
				.size(100,100)
				.attr("viewbox", "0 0 100 100")
				.style("left: "+(30*x)+"px; top: "+(30*y)+"px");

		new Tag("rect")
				.size(30,100)
				.pos(35,0)
				.addTo(svg);
		
		new Tag("polygon")
		.attr("points","35,100 100,35 100,65 65,100")
		.addTo(svg);
		
		return svg.toString();
	}
}
