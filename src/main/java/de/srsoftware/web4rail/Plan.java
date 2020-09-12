package de.srsoftware.web4rail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tiles.DiagES;
import de.srsoftware.web4rail.tiles.DiagNE;
import de.srsoftware.web4rail.tiles.DiagSW;
import de.srsoftware.web4rail.tiles.DiagWN;
import de.srsoftware.web4rail.tiles.EndE;
import de.srsoftware.web4rail.tiles.EndW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.TurnoutSE;
import de.srsoftware.web4rail.tiles.TurnoutSW;
import de.srsoftware.web4rail.tiles.TurnoutWS;

public class Plan {
	private static final String MODE = "mode";
	private static final String MODE_ADD = "1";
	private static final String TILE = "tile";
	private static final Logger LOG = LoggerFactory.getLogger(Plan.class);
	private static final String X = "x";
	private static final String Y = "y";
	
	private HashMap<Integer,HashMap<Integer,Tile>> tiles = new HashMap<Integer,HashMap<Integer,Tile>>();
	
	private Tile addTile(String clazz, String xs, String ys) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		int x = Integer.parseInt(xs);
		int y = Integer.parseInt(ys);
		if (clazz == null) throw new NullPointerException(TILE+" must not be null!");
		Class<Tile> tc = Tile.class;
		clazz = tc.getName().replace(".Tile", "."+clazz);
		Tile tile = (Tile) tc.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		set(x, y, tile);
		return tile;
	}
	
	public Tile get(int x, int y) {
		HashMap<Integer, Tile> column = tiles.get(x);
		return column == null ? null : column.get(y);
	}

	public Page html() throws IOException {
		Page page = new Page();
		for (Entry<Integer, HashMap<Integer, Tile>> column : tiles.entrySet()) {
			int x = column.getKey();
			for (Entry<Integer, Tile> row : column.getValue().entrySet()) {
				int y = row.getKey();
				Tile tile = row.getValue().position(x, y);
				page.append("\t\t"+tile.html()+"\n");
			}
		}
		return page.append(menu()).append(messages());
	}

	private Tag messages() {
		return new Tag("div").id("messages").content("");
	}

	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		
		tileMenu().addTo(menu);
		actionMenu().addTo(menu);
			
		return menu;
	}

	private Tag actionMenu() {
		Tag menu = new Tag("div").clazz("actions").content("Actions");
		return menu;
	}

	public String process(HashMap<String, String> params) {
		try {
			String mode = params.get(MODE);
			
			if (mode == null) throw new NullPointerException(MODE+" should not be null!");
			switch (mode) {
				case MODE_ADD:
					Tile tile = addTile(params.get(TILE),params.get(X),params.get(Y));
					return t("Added {}",tile.getClass().getSimpleName());
				default:
					LOG.warn("Unknown mode: {}",mode);
			}
			return t("unknown mode: {}",mode);
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public Tile set(int x,int y,Tile tile) {
		Tile old = null;
		HashMap<Integer, Tile> column = tiles.get(x);
		if (column == null) {
			column = new HashMap<Integer,Tile>();
			tiles.put(x, column);
		}
		old = column.get(y);
		column.put(y,tile.position(x, y));
		return old;
	}
	
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	private Tag tileMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("addtile").content("Add tile");
		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new StraightH().html());
		tiles.append(new StraightV().html());
		tiles.append(new DiagES().html());
		tiles.append(new DiagSW().html());
		tiles.append(new DiagNE().html());
		tiles.append(new DiagWN().html());
		tiles.append(new EndE().html());
		tiles.append(new EndW().html());
		tiles.append(new TurnoutSE().html());
		tiles.append(new TurnoutWS().html());
		tiles.append(new TurnoutSW().html());
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
}
