package de.srsoftware.web4rail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

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
import de.srsoftware.web4rail.tiles.Eraser;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.TurnoutEN;
import de.srsoftware.web4rail.tiles.TurnoutES;
import de.srsoftware.web4rail.tiles.TurnoutSE;
import de.srsoftware.web4rail.tiles.TurnoutSW;
import de.srsoftware.web4rail.tiles.TurnoutWN;
import de.srsoftware.web4rail.tiles.TurnoutWS;

public class Plan {
	private static final String ACTION = "action";
	private static final String ACTION_ADD = "add";
	private static final String ACTION_SAVE = "save";
	private static final String TILE = "tile";
	private static final Logger LOG = LoggerFactory.getLogger(Plan.class);
	private static final String X = "x";
	private static final String Y = "y";
	private static final String NAME = "name";
	private static final String ACTION_PROPS = "openProps";
	private static final String ACTION_MOVE = "move";
	private static final String DIRECTION = "direction";
	private static final String EAST = "east";
	private static final String WEST = "west";
	private static final String SOUTH = "south";
	private static final String NORTH = "north";
	
	private HashMap<Integer,HashMap<Integer,Tile>> tiles = new HashMap<Integer,HashMap<Integer,Tile>>();
	
	private Tag actionMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("actions").content(t("Actions"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Tag("div").id("save").content(t("Save plan")));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
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
				if (tile != null) page.append("\t\t"+tile.html()+"\n");
			}
		}
		return page.append(menu()).append(messages());
	}
	
	public static Plan load(String filename) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Plan result = new Plan();
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()) {
			String line = br.readLine().trim();
			String[] parts = line.split(":");
			result.addTile(parts[2].trim(), parts[0].trim(), parts[1].trim());
		}
		br.close();
		return result;
	}

	private Tag messages() {
		return new Tag("div").id("messages").content("");
	}

	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		actionMenu().addTo(menu);
		moveMenu().addTo(menu);		
		tileMenu().addTo(menu);
		return menu;
	}

	private Tag moveMenu() {
		Tag tileMenu = new Tag("div").clazz("move").title(t("Move tiles")).content(t("↹"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Tag("div").id("west").title(t("Move west")).content("🢀"));
		tiles.append(new Tag("div").id("east").title(t("Move east")).content("🢂"));
		tiles.append(new Tag("div").id("north").title(t("Move north")).content("🢁"));
		tiles.append(new Tag("div").id("south").title(t("Move south")).content("🢃"));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
	private String moveTile(String direction, String x, String y) throws NumberFormatException, IOException {
		return moveTile(direction,Integer.parseInt(x),Integer.parseInt(y));
	}

	private String moveTile(String direction, int x, int y) throws IOException {
		LOG.debug("moveTile({},{},{})",direction,x,y);
		Vector<Tile> moved = null;
		switch (direction) {
			case EAST:
				moved = moveTile(x,y,+1,0);
				break;
			case WEST:
				moved = moveTile(x,y,-1,0);
				break;
			case NORTH:
				moved = moveTile(x,y,0,-1);
				break;
			case SOUTH:
				moved = moveTile(x,y,0,+1);
				break;
		}
		if (!moved.isEmpty()) {
			set(x,y,null);
			StringBuilder sb = new StringBuilder();
			for (Tile tile : moved) sb.append(tile.html()+"\n");
			return sb.toString();
		}		
		return null;
	}

	private Vector<Tile> moveTile(int x, int y,int xstep,int ystep) {
		LOG.debug("moveEast({},{})",x,y);
		Tile tile = this.get(x, y);
		if (tile == null) return new Vector<Tile>();
		Vector<Tile> result = moveTile(x+xstep,y+ystep,xstep,ystep);
		set(x+xstep, y+ystep, tile);
		result.add(tile);
		return result;
	}
	
	public Object process(HashMap<String, String> params) {
		try {
			String action = params.get(ACTION);
			
			if (action == null) throw new NullPointerException(ACTION+" should not be null!");
			switch (action) {
				case ACTION_ADD:
					Tile tile = addTile(params.get(TILE),params.get(X),params.get(Y));
					return t("Added {}",tile.getClass().getSimpleName());
				case ACTION_MOVE:
					return moveTile(params.get(DIRECTION),params.get(X),params.get(Y));
				case ACTION_PROPS:
					return Tile.propMenu();
				case ACTION_SAVE:
					return saveTo(params.get(NAME));
				default:
					LOG.warn("Unknown action: {}",action);
			}
			return t("Unknown action: {}",action);
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private String saveTo(String name) throws IOException {
		if (name == null || name.isEmpty()) throw new NullPointerException("Name must not be empty!");
		File file = new File(name+".plan");
		BufferedWriter br = new BufferedWriter(new FileWriter(file));
		for (Entry<Integer, HashMap<Integer, Tile>> column : tiles.entrySet()) {
			int x = column.getKey();
			for (Entry<Integer, Tile> row : column.getValue().entrySet()) {
				int y = row.getKey();
				Tile tile = row.getValue().position(x, y);
				if (tile != null) br.append(x+":"+y+":"+tile.getClass().getSimpleName()+"\n");
			}
		}
		br.close();
		return t("Plan saved as \"{}\".",file);
	}

	public Tile set(int x,int y,Tile tile) {
		Tile old = null;
		HashMap<Integer, Tile> column = tiles.get(x);
		if (column == null) {
			column = new HashMap<Integer,Tile>();
			tiles.put(x, column);
		}
		old = column.remove(y);
		if (tile != null && !(tile instanceof Eraser)) column.put(y,tile.position(x, y));
		return old;
	}
	
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	private Tag tileMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("addtile").title(t("Add tile")).content("◫");
		
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
		tiles.append(new TurnoutEN().html());
		tiles.append(new TurnoutES().html());
		tiles.append(new TurnoutWS().html());
		tiles.append(new TurnoutWN().html());
		tiles.append(new TurnoutSW().html());
		tiles.append(new Eraser().html());
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
}
