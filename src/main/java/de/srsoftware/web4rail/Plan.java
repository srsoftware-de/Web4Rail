package de.srsoftware.web4rail;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Div;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.BlockH;
import de.srsoftware.web4rail.tiles.BlockV;
import de.srsoftware.web4rail.tiles.Contact;
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
import de.srsoftware.web4rail.tiles.Turnout3E;
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
		NORTH, SOUTH, EAST, WEST;
		
		public Direction inverse() {
			switch (this) {
			case NORTH: return SOUTH;
			case SOUTH: return NORTH;
			case EAST: return WEST;
			case WEST: return EAST;
			}
			return null;
		}
	}
	
	private class Heartbeat extends Thread {
		@Override
		public void run() {			
			try {
				while (true) {
					sleep(10000);
					heatbeat();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final String ACTION = "action";
	private static final String ACTION_ADD = "add";
	private static final String ACTION_ANALYZE = "analyze";
	private static final String ACTION_MOVE = "move";
	private static final String ACTION_CLICK = "click";
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
	private static final String ROUTE = "route";
	private static final HashMap<OutputStreamWriter,Integer> clients = new HashMap<OutputStreamWriter, Integer>();
	private static final String ACTION_TRAIN = "train";
	private static final String ACTION_LOCOS = "locos";
	private static final String ACTION_TRAINS = "trains";
	private static final String ACTION_CAR = "car";
	public static final String ACTION_ADD_LOCO = "addLoco";
	public static final String ACTION_ADD_TRAIN = "addTrain";
	
	public HashMap<String,Tile> tiles = new HashMap<String,Tile>();
	private HashSet<Block> blocks = new HashSet<Block>();
	private HashMap<Integer, Route> routes = new HashMap<Integer, Route>();
	
	public Plan() {
		new Heartbeat().start();
	}

	private Tag actionMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("actions").content(t("Actions"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Div("save").content(t("Save plan")));
		tiles.append(new Div("analyze").content(t("Analyze plan")));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
	public void addClient(OutputStreamWriter client) {
		LOG.debug("Client connected.");
		clients.put(client, 0);
	}
	
	public static void addLink(Tile tile,String content,Tag list) {
		new Tag("li").clazz("link").attr("onclick", "return clickTile("+tile.x+","+tile.y+");").content(content).addTo(list);
	}
	
	private String addTile(String clazz, String xs, String ys, String configJson) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {
		int x = Integer.parseInt(xs);
		int y = Integer.parseInt(ys);
		if (clazz == null) throw new NullPointerException(TILE+" must not be null!");
		Class<Tile> tc = Tile.class;		
		clazz = tc.getName().replace(".Tile", "."+clazz);		
		Tile tile = (Tile) tc.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		if (tile instanceof Eraser) {
			Tile erased = get(Tile.id(x,y),true);
			remove(erased);
			return erased == null ? null : t("Removed {}.",erased);
		}
		//if (configJson != null) tile.configure(new JSONObject(configJson));		
		set(x, y, tile);
		return t("Added {}",tile.getClass().getSimpleName());
	}
	
	private String analyze() {
		Vector<Route> routes = new Vector<Route>();
		for (Block block : blocks) {
			for (Connector con : block.startPoints()) routes.addAll(follow(new Route().start(block,con.from.inverse()),con));
		}
		this.routes.clear();
		for (Tile tile : tiles.values()) tile.routes().clear();
		for (Route route : routes) {
			route.complete();
			registerRoute(route);
		}
		return t("Found {} routes.",routes.size());
	}

	public Collection<Block> blocks() {
		return blocks;
	}
	
	private Object carAction(HashMap<String, String> params) {
		Car car = Car.get(params.get(Car.ID));
		if (car == null) return t("No car with id {} found!",params.get(Car.ID));
		return car.properties();
	}
	
	private Object click(Tile tile) throws IOException {
		if (tile == null) return null;
		return tile.click();
	}

	private Collection<Route> follow(Route route, Connector connector) {
		Tile tile = get(Tile.id(connector.x,connector.y),false);
		Vector<Route> results = new Vector<>();
		if (tile == null) return results;
		Tile addedTile = route.add(tile,connector.from);
		if (addedTile instanceof Block) {
			Map<Connector, State> cons = addedTile.connections(connector.from);
			LOG.debug("Found {}, coming from {}.",addedTile,connector.from);
			for (Connector con : cons.keySet()) { // falls direkt nach dem Block noch ein Kontakt kommt: diesen mit zu Route hinzufügen
				LOG.debug("This is connected to {}",con);
				Tile nextTile = get(Tile.id(con.x,con.y),false);
				if (nextTile instanceof Contact) {
					LOG.debug("{} is followed by {}",addedTile,nextTile);
					route.add(nextTile, con.from);
				}
				break;
			}
			return List.of(route);
		}
		Map<Connector, State> connectors = tile.connections(connector.from);
		List<Route>routes = route.multiply(connectors.size());
		LOG.debug("{}",tile);
		if (connectors.size()>1) LOG.debug("SPLITTING @ {}",tile);
		
		for (Entry<Connector, State> entry: connectors.entrySet()) {
			route = routes.remove(0);
			connector = entry.getKey();
			route.setLast(entry.getValue());
			if (connectors.size()>1) LOG.debug("RESUMING from {}",tile);
			results.addAll(follow(route,connector));
		}
		
		return results;
	}
	
	public Tile get(String tileId,boolean resolveShadows) {
		Tile tile = tiles.get(tileId);
		if (resolveShadows && tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		return tile;
	}
	
	private Tag heartbeat() {
		return new Div("heartbeat").content("");
	}

	public void heatbeat() {
		stream("heartbeat @ "+new Date().getTime());
	}
	
	public Page html() throws IOException {
		Page page = new Page().append("<div id=\"plan\">");
		for (Tile tile: tiles.values()) {
			if (tile == null) continue;
			page.append("\t\t"+tile.tag(null)+"\n");
		}
		return page
				.append(menu())
				.append(messages())
				.append(heartbeat())
				.append("</div>")
				.style("css/style.css")
				.js("js/jquery-3.5.1.min.js")
				.js("js/plan.js");
	}
	
	public static Plan load(String filename) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		try {
			Car.loadAll(filename+".cars");
		} catch (Exception e) {
			LOG.debug("Was not able to load cars!",e);
		}
		try {
			Train.loadAll(filename+".trains");
		} catch (Exception e) {
			LOG.debug("Was not able to load trains!",e);
		}
		Plan plan = new Plan();
		Tile.loadAll(filename+".plan",plan);
		try {
			Route.loadAll(filename+".routes",plan);
		} catch (Exception e) {
			LOG.debug("Was not able to load routes!",e);
		}
		return plan;
	}

	private Object locoAction(HashMap<String, String> params) throws IOException {
		switch (params.get(ACTION)) {
			case ACTION_ADD_LOCO:
				new Locomotive(params.get(Locomotive.NAME));
				break;
		}
		
		return html();
	}
	
	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		actionMenu().addTo(menu);
		moveMenu().addTo(menu);		
		tileMenu().addTo(menu);
		trainMenu().addTo(menu);
		return menu;
	}

	private Tag messages() {
		return new Div("messages").content("");
	}
	
	private Tag moveMenu() {
		Tag tileMenu = new Tag("div").clazz("move").title(t("Move tiles")).content(t("↹"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Div("west").title(t("Move west")).content("↤"));
		tiles.append(new Div("east").title(t("Move east")).content("↦"));
		tiles.append(new Div("north").title(t("Move north")).content("↥"));
		tiles.append(new Div("south").title(t("Move south")).content("↧"));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
	private String moveTile(String direction, String tileId) throws NumberFormatException, IOException {
		switch (direction) {
		case "south":
			return moveTile(get(tileId,false),Direction.SOUTH);
		case "north":
			return moveTile(get(tileId,false),Direction.NORTH);
		case "east":
			return moveTile(get(tileId,false),Direction.EAST);
		case "west":
			return moveTile(get(tileId,false),Direction.WEST);
		}
		throw new InvalidParameterException(t("\"{}\" is not a known direction!"));
	}

	private String moveTile(Tile tile, Direction direction) throws IOException {
		boolean moved = false;
		if (tile != null) {
			LOG.debug("moveTile({},{},{})",direction,tile.x,tile.y);
			switch (direction) {		
				case EAST:
					moved = moveTile(tile,+1,0);
					break;
				case WEST:
					moved = moveTile(tile,-1,0);
					break;
				case NORTH:
					moved = moveTile(tile,0,-1);
					break;
				case SOUTH:
					moved = moveTile(tile,0,+1);
					break;
			}
		}
		return t(moved ? "Tile(s) moved.":"No tile(s) moved.");
	}

	private boolean moveTile(Tile tile,int xstep,int ystep) throws IOException {
		LOG.error("moveTile({}  +{}/+{})",tile,xstep,ystep);
		Stack<Tile> stack = new Stack<Tile>();
		while (tile != null) {
			LOG.debug("scheduling tile for movement: {}",tile);
			stack.add(tile);
			tile = get(Tile.id(tile.x+xstep, tile.y+ystep),false);
		}
		while (!stack.isEmpty()) {
			tile = stack.pop();
			if (!(tile instanceof Shadow)) {
				LOG.debug("altering position of {}",tile);
				remove(tile);
				set(tile.x+xstep,tile.y+ystep,tile);
			}
		}
		return false;
	}
	
	public void place(Tile tile) throws IOException {
		stream("place "+tile.tag(null));
	}
	
	public Object process(HashMap<String, String> params) {
		try {
			String action = params.get(ACTION);
			
			if (action == null) throw new NullPointerException(ACTION+" should not be null!");
			switch (action) {
				case ACTION_ADD:
					return addTile(params.get(TILE),params.get(X),params.get(Y),null);
				case ACTION_ADD_LOCO:
					return locoAction(params);
				case ACTION_ADD_TRAIN:
				case ACTION_TRAIN:
					return trainAction(params);
				case ACTION_CAR:
					return carAction(params);
				case ACTION_CLICK:
					return click(get(params.get(Tile.ID),true));
				case ACTION_ANALYZE:
					return analyze();
				case ACTION_LOCOS:
					return Locomotive.manager();
				case ACTION_MOVE:
					return moveTile(params.get(DIRECTION),params.get(Tile.ID));
				case ACTION_ROUTE:
					return routeProperties(Integer.parseInt(params.get(ID)));
				case ACTION_SAVE:
					return saveTo(params.get(FILE));
				case ACTION_TRAINS:
					return Train.manager();
				case ACTION_UPDATE:
					return update(params);		
				default:
					LOG.warn("Unknown action: {}",action);
			}
			return t("Unknown action: {}",action);
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null || msg.isEmpty()) msg = t("An unknown error occured!");
			LOG.debug(msg,e);
			return msg;
		}
	}

	private Object trainAction(HashMap<String, String> params) throws IOException {
		LOG.debug("Params: {}",params);
		switch (params.get(ACTION)) {
			case ACTION_ADD_TRAIN:
				Locomotive loco = (Locomotive) Locomotive.get(params.get(Train.LOCO_ID));
				if (loco == null) return t("unknown locomotive: {}",params.get(Locomotive.ID));
				new Train(loco);
				break;
			case ACTION_TRAIN:
				Object result = Train.action(params);
				if (!(result instanceof Train)) return result;
				break;
		}
		return html();
	}
	
	public Route route(int routeId) {
		return routes.get(routeId);
	}

	private Object routeProperties(int id) {
		Route route = route(id);
		if (route == null) return t("Could not find route \"{}\"",id);
		return route.properties();
	}
	
	Route registerRoute(Route route) {
		for (Tile tile: route.path()) tile.add(route);
		routes.put(route.id(), route);
		return route;
	}
	
	private void remove(Tile tile) {
		remove_intern(tile.x,tile.y);
		if (tile instanceof Block) blocks.remove(tile);
		for (int i=1; i<tile.len(); i++) remove_intern(tile.x+i, tile.y); // remove shadow tiles
		for (int i=1; i<tile.height(); i++) remove_intern(tile.x, tile.y+i); // remove shadow tiles
		if (tile != null) stream("remove "+tile.id());
	}

	private void remove_intern(int x, int y) {
		LOG.debug("removed {} from tile list",tiles.remove(Tile.id(x, y)));
	}
	
	private String saveTo(String name) throws IOException {
		if (name == null || name.isEmpty()) throw new NullPointerException("Name must not be empty!");
		Car.saveAll(name+".cars");
		Tile.saveAll(tiles,name+".plan");
		Train.saveAll(name+".trains"); // refers to cars, blocks
		Route.saveAll(routes.values(),name+".routes"); // refers to tiles
		return t("Plan saved as \"{}\".",name);
	}
	
	public void set(int x,int y,Tile tile) throws IOException {
		if (tile == null) return;
		if (tile instanceof Block) blocks.add((Block) tile);
		for (int i=1; i<tile.len(); i++) set(x+i,y,new Shadow(tile));
		for (int i=1; i<tile.height(); i++) set(x,y+i,new Shadow(tile));
		set_intern(x,y,tile);
		place(tile);		
	}
	
	private void set_intern(int x, int y, Tile tile) {
		tile.position(x, y).plan(this);
		tiles.put(tile.id(),tile);
	}
	
	public synchronized void stream(String data) {
		data = data.replaceAll("\n", "").replaceAll("\r", "");
		//LOG.debug("streaming: {}",data);
		Vector<OutputStreamWriter> badClients = null;
		for (Entry<OutputStreamWriter, Integer> entry : clients.entrySet()) {
			OutputStreamWriter client = entry.getKey();
			try {
				client.write("data: "+data+"\n\n");
				client.flush();
				clients.put(client,0);
			} catch (IOException e) {
				int errorCount = entry.getValue()+1;
				LOG.info("Error #{} on client: {}",errorCount,e.getMessage());
				if (errorCount > 4) {
					if (badClients == null) badClients = new Vector<OutputStreamWriter>();
					try {
						client.close();
					} catch (IOException e1) {}
					badClients.add(client);
				} else clients.put(client,errorCount);
			}
		}
		if (badClients != null) for (OutputStreamWriter client: badClients) {
			LOG.info("Disconnecting client.");
			clients.remove(client);			
		}
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
		tiles.append(new Turnout3E().tag(null));
		tiles.append(new CrossH().tag(null));
		tiles.append(new CrossV().tag(null));
		tiles.append(new Eraser().tag(null));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}
	
	private Tag trainMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("trains").content(t("Trains"));		
		StringBuffer tiles = new StringBuffer();
		tiles.append(new Div("trains").content(t("Manage trains")));
		tiles.append(new Div("locos").content(t("Manage locos")));
		return new Tag("div").clazz("list").content(tiles.toString()).addTo(tileMenu);
	}

	private Object update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(ROUTE)) {
			Route route = routes.get(Integer.parseInt(params.get(ROUTE)));
			if (route == null) return t("Unknown route: {}",params.get(ROUTE));
			route.update(params);
		} else update(get(params.get(Tile.ID),true),params);
		return this.html();
	}

	private void update(Tile tile, HashMap<String, String> params) throws IOException {
		if (tile != null) place(tile.update(params));
	}

	public void warn(Contact contact) {
		stream(t("Warning: {}",t("Ghost train @ {}",contact)));
	}
}
