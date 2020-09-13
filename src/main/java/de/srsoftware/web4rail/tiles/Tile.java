package de.srsoftware.web4rail.tiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Scanner;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.Window;

public abstract class Tile {
	
	protected int x = -1,y = -1;
	protected HashSet<String> classes = new HashSet<String>();
	
	public Tile() {
		classes.add("tile");
		classes.add(getClass().getSimpleName());
	}
	
	public int height() {
		return 1;
	}
	
	public int len() {
		return 1;
	}
	
	public Tile position(int x, int y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	public static Tag propMenu() {
		return new Window("tile-properties",t("Properties")).content(t("This tile has no properties"));
	}

	public Tag tag() throws IOException {
		
		Tag svg = new Tag("svg")
				.id((x!=-1 && y!=-1)?("tile-"+x+"-"+y):(getClass().getSimpleName()))
				.clazz(classes)
				.size(100,100)
				.attr("name", getClass().getSimpleName())
				.attr("viewbox", "0 0 100 100");
				if (x>-1) svg.style("left: "+(30*x)+"px; top: "+(30*y)+"px");

		File file = new File(System.getProperty("user.dir")+"/resources/svg/"+getClass().getSimpleName()+".svg");
		if (file.exists()) {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			StringBuffer sb = new StringBuffer();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("<svg") || line.endsWith("svg>")) continue;
				sb.append(line+"\n");
			}
			scanner.close();
			svg.content(sb.toString());
		} else {
			new Tag("title").content(t("No display defined for this tile ({})",getClass().getSimpleName())).addTo(svg);
			new Tag("text")
				.pos(35,70)	
				.content("?")
				.addTo(svg);
		}
		
		return svg;
	}

	private static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}	
	
	@Override
	public String toString() {
		return t("{}({},{})",getClass().getSimpleName(),x,y) ;
	}
}
