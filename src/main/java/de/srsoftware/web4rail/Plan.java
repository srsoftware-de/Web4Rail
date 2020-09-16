package de.srsoftware.web4rail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.BlockH;
import de.srsoftware.web4rail.tiles.BlockV;
import de.srsoftware.web4rail.tiles.ContactH;
import de.srsoftware.web4rail.tiles.ContactV;
import de.srsoftware.web4rail.tiles.CrossH;
import de.srsoftware.web4rail.tiles.CrossV;
import de.srsoftware.web4rail.tiles.DiagES;
import de.srsoftware.web4rail.tiles.DiagNE;
import de.srsoftware.web4rail.tiles.DiagSW;
import de.srsoftware.web4rail.tiles.DiagWN;
import de.srsoftware.web4rail.tiles.EndE;
import de.srsoftware.web4rail.tiles.EndN;
import de.srsoftware.web4rail.tiles.EndS;
import de.srsoftware.web4rail.tiles.EndW;
import de.srsoftware.web4rail.tiles.Eraser;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.SignalE;
import de.srsoftware.web4rail.tiles.SignalN;
import de.srsoftware.web4rail.tiles.SignalS;
import de.srsoftware.web4rail.tiles.SignalW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout.State;
import de.srsoftware.web4rail.tiles.TurnoutLE;
import de.srsoftware.web4rail.tiles.TurnoutLN;
import de.srsoftware.web4rail.tiles.TurnoutLS;
import de.srsoftware.web4rail.tiles.TurnoutLW;
import de.srsoftware.web4rail.tiles.TurnoutRE;
import de.srsoftware.web4rail.tiles.TurnoutRN;
import de.srsoftware.web4rail.tiles.TurnoutRS;
import de.srsoftware.web4rail.tiles.TurnoutRW;

public class Plan {
	public enum Direction{		
		NORTH, SOUTH, EAST, WEST
	}
	private static final String ACTION = "action";
	private static final String ACTION_ADD = "add";
	private static final String ACTION_ANALYZE = "analyze";
	private static final String ACTION_MOVE = "move";
	private static final String ACTION_PROPS = "openProps";
	private static final String ACTION_SAVE = "save";
	private static final String ACTION_UPDATE = "update";
	private static final String TILE = "tile";
	private static final Logger LOG = LoggerFactory.getLogger(Plan.class);
	private static final String X = "x";
	private static final String Y = "y";
	private static final String FILE = "file";
	private static final String DIRECTION = "direction";
	private static final String ACTION_ROUTE = "openRoute";
	private static final String ID = "id";
	
	private HashMap<Integer,HashMap<Integer,Tile>> tiles = new HashMap<Integer,HashMap<Integer,Tile>>();
	private HashSet<Block> blocks = new HashSet<Block>();
	private HashMap<String, Route> routes = new HashMap<String, Route>();
	
	private Tag actionMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("actions").content(t("Actions"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Tag("div").id("save").content(t("Save plan")));
		tiles.append(new Tag("div").id("analyze").content(t("Analyze plan")));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
	private Tile addTile(String clazz, String xs, String ys, String configJson) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		int x = Integer.parseInt(xs);
		int y = Integer.parseInt(ys);
		if (clazz == null) throw new NullPointerException(TILE+" must not be null!");
		Class<Tile> tc = Tile.class;		
		clazz = tc.getName().replace(".Tile", "."+clazz);		
		Tile tile = (Tile) tc.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		if (configJson != null) tile.configure(new JSONObject(configJson));		
		set(x, y, tile);		
		return tile;
	}
	
	private String analyze() {
		Vector<Route> routes = new Vector<Route>();
		for (Block block : blocks) {
			block.routes().clear();
			for (Connector con : block.startPoints()) routes.addAll(follow(new Route().start(block),con));
		}
		this.routes.clear();
		for (Route route : routes) {
			route.start().add(route);
			this.routes.put(route.id(), route);
		}
		return t("Found {} routes.",routes.size());
	}
	
	private Collection<Route> follow(Route route, Connector connector) {
		Tile tile = get(connector.x,connector.y);
		Vector<Route> results = new Vector<>();
		if (tile == null) return results;
		Tile addedTile = route.add(tile,connector.from);
		if (addedTile instanceof Block) return List.of(route);
		Map<Connector, State> connectors = tile.connections(connector.from);
		List<Route>routes = route.multiply(connectors.size());
		LOG.debug("{}",tile);
		if (connectors.size()>1) LOG.debug("SPLITTING @ {}",tile);
		
		for (Entry<Connector, State> entry: connectors.entrySet()) {
			route = routes.remove(0);
			connector = entry.getKey();
			State state = entry.getValue();
			route.setLast(state);
			if (connectors.size()>1) {
				LOG.debug("RESUMING from {}",tile);
			}
			results.addAll(follow(route,connector));
		}
		
		return results;
	}

	public Tile get(int x, int y) {
		HashMap<Integer, Tile> column = tiles.get(x);
		return column == null ? null : column.get(y);
	}

	public Page html() throws IOException {
		Page page = new Page().append("<div id=\"plan\">");
		for (Entry<Integer, HashMap<Integer, Tile>> column : tiles.entrySet()) {
			int x = column.getKey();
			for (Entry<Integer, Tile> row : column.getValue().entrySet()) {
				int y = row.getKey();
				Tile tile = row.getValue().position(x, y);
				if (tile != null) page.append("\t\t"+tile.tag(null)+"\n");
			}
		}
		return page.append(menu()).append(messages()).append("</div>").style("css/style.css").js("js/jquery-3.5.1.min.js").js("js/plan.js");
	}
	
	public static Plan load(String filename) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Plan result = new Plan();
		File file = new File(filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()) {
			String line = br.readLine().trim();
			String[] parts = line.split(":",4);
			try {
				String x = parts[0];
				String y = parts[1];
				String clazz = parts[2];
				result.addTile(clazz, x, y, parts.length>3 ? parts[3] : null);
			} catch (Exception e) {
				LOG.warn("Was not able to load \"{}\":",line,e);
			}
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
		switch (direction) {
		case "south":
			return moveTile(Direction.SOUTH,Integer.parseInt(x),Integer.parseInt(y));
		case "north":
			return moveTile(Direction.NORTH,Integer.parseInt(x),Integer.parseInt(y));
		case "east":
			return moveTile(Direction.EAST,Integer.parseInt(x),Integer.parseInt(y));
		case "west":
			return moveTile(Direction.WEST,Integer.parseInt(x),Integer.parseInt(y));
		}
		throw new InvalidParameterException(t("\"{}\" is not a known direction!"));
	}

	private String moveTile(Direction direction, int x, int y) throws IOException {
		//LOG.debug("moveTile({},{},{})",direction,x,y);
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
			for (Tile tile : moved) sb.append(tile.tag(null)+"\n");
			return sb.toString();
		}		
		return null;
	}

	private Vector<Tile> moveTile(int x, int y,int xstep,int ystep) {
		LOG.debug("moveTile({}+{},{}+{})",x,xstep,y,ystep);
		if (x+xstep < -1 || y+ystep < -1) {
			throw new IndexOutOfBoundsException("Can not move tile out of screen!");
		}
		Tile tile = this.get(x, y);
		if (tile == null) return new Vector<Tile>();
		Vector<Tile> result = moveTile(x+xstep,y+ystep,xstep,ystep);
		set(x+xstep,y+ystep, tile);
		result.add(tile);
		return result;
	}
	
	public Object process(HashMap<String, String> params) {
		try {
			String action = params.get(ACTION);
			
			if (action == null) throw new NullPointerException(ACTION+" should not be null!");
			switch (action) {
				case ACTION_ADD:
					Tile tile = addTile(params.get(TILE),params.get(X),params.get(Y),null);
					return t("Added {}",tile.getClass().getSimpleName());
				case ACTION_ANALYZE:
					return analyze();
				case ACTION_MOVE:
					return moveTile(params.get(DIRECTION),params.get(X),params.get(Y));
				case ACTION_PROPS:
					return propMenu(params.get(X),params.get(Y));
				case ACTION_ROUTE:
					return routeProperties(params.get(ID));
				case ACTION_SAVE:
					return saveTo(params.get(FILE));
				case ACTION_UPDATE:
					return update(params);
				default:
					LOG.warn("Unknown action: {}",action);
			}
			return t("Unknown action: {}",action);
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null || msg.isEmpty()) msg = t("An unknown error occured!");
			return msg;
		}
	}

	private Object routeProperties(String routeId) {
		Route route = routes.get(routeId);
		if (route == null) return t("Could not find route \"{}\"",routeId);
		return route.properties();
	}

	private Page update(HashMap<String, String> params) throws IOException {
		return update(Integer.parseInt(params.get("x")),Integer.parseInt(params.get("y")),params);
	}

	private Page update(int x,int y, HashMap<String, String> params) throws IOException {
		Tile tile = get(x,y);
		if (tile != null) set(x,y,tile.update(params));
		return this.html();
	}

	private Tag propMenu(String x, String y) {
		return propMenu(Integer.parseInt(x),Integer.parseInt(y));
	}

	private Tag propMenu(int x, int y) {
		Tile tile = get(x, y);
		if (tile == null) return null;
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		return tile.propMenu();
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
				if (tile != null && !(tile instanceof Shadow)) {
					br.append(x+":"+y+":"+tile.getClass().getSimpleName());
					JSONObject config = tile.config();
					if (!config.isEmpty()) br.append(":"+config);
					br.append("\n");
				}
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
		if (old instanceof Block) blocks.remove((Block)old);
		if (tile != null && !(tile instanceof Eraser)) {
			column.put(y,tile.position(x, y));
			for (int i=1; i<tile.len(); i++) set(x+i,y,new Shadow(tile));
			for (int i=1; i<tile.height(); i++) set(x,y+i,new Shadow(tile));
			if (tile instanceof Block) blocks.add((Block)tile);
		}
		return old;
	}
	
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	private Tag tileMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("addtile").title(t("Add tile")).content("◫");
		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new StraightH().tag(null));
		tiles.append(new StraightV().tag(null));
		tiles.append(new ContactH().tag(null));
		tiles.append(new ContactV().tag(null));
		tiles.append(new SignalW().tag(null));
		tiles.append(new SignalE().tag(null));
		tiles.append(new SignalS().tag(null));
		tiles.append(new SignalN().tag(null));
		tiles.append(new BlockH().tag(null));
		tiles.append(new BlockV().tag(null));
		tiles.append(new DiagES().tag(null));
		tiles.append(new DiagSW().tag(null));
		tiles.append(new DiagNE().tag(null));
		tiles.append(new DiagWN().tag(null));
		tiles.append(new EndE().tag(null));
		tiles.append(new EndW().tag(null));
		tiles.append(new EndN().tag(null));
		tiles.append(new EndS().tag(null));
		tiles.append(new TurnoutRS().tag(null));
		tiles.append(new TurnoutRN().tag(null));
		tiles.append(new TurnoutRW().tag(null));
		tiles.append(new TurnoutRE().tag(null));
		tiles.append(new TurnoutLN().tag(null));
		tiles.append(new TurnoutLS().tag(null));
		tiles.append(new TurnoutLW().tag(null));
		tiles.append(new TurnoutLE().tag(null));
		tiles.append(new CrossH().tag(null));
		tiles.append(new CrossV().tag(null));
		tiles.append(new Eraser().tag(null));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
}
