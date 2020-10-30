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

/**
 * This class is a central part of the Application, as it loads, holds and saves all kinds of information:
 * <ul>
 *   <li>Tack layout</li>
 *   <li>Trains and Cars</li>
 *   <li>Routes</li>
 *   <li>...</li>
 * </ul>
 * @author Stephan Richter, SRSoftware
 *
 */
public class Plan implements Constants{
	/**
	 * The four directions Trains can be within blocks
	 */
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
	
	/**
	 * This thread sends a heartbea to the client
	 */
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
	

	private static final String TILE = "tile";
	private static final Logger LOG = LoggerFactory.getLogger(Plan.class);
	private static final String X = "x";
	private static final String Y = "y";
	private static final String DIRECTION = "direction";
	private static final HashMap<OutputStreamWriter,Integer> clients = new HashMap<OutputStreamWriter, Integer>();
	private static final String ACTION_QR = "qrcode";
	
	public HashMap<String,Tile> tiles = new HashMap<String,Tile>(); // The list of tiles of this plan, i.e. the Track layout
	private HashSet<Block> blocks = new HashSet<Block>(); // the list of tiles, that are blocks
	private HashMap<Integer, Route> routes = new HashMap<Integer, Route>(); // the list of routes of the track layout
	private ControlUnit controlUnit = new ControlUnit(this); // the control unit, to which the plan is connected 
	
	/**
	 * creates a new plan, starts to send heart beats
	 */
	public Plan() {
		new Heartbeat().start();
	}
	
	/**
	 * manages plan-related commands
	 * @param params the parameters passed from the client
	 * @return Object returned to the client
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public Object action(HashMap<String, String> params) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		switch (params.get(ACTION)) {
		case ACTION_ADD:
			return addTile(params.get(TILE),params.get(X),params.get(Y),null);
		case ACTION_ANALYZE:
			return analyze();
		case ACTION_CLICK:
			return click(get(params.get(Tile.ID),true));
		case ACTION_MOVE:
			return moveTile(params.get(DIRECTION),params.get(Tile.ID));
		case ACTION_SAVE:
			return saveTo("default");
		case ACTION_UPDATE:
			return update(get(params.get(Tile.ID),true),params);
		}
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	/**
	 * generates the action menu that is appended to the plan
	 * @return
	 * @throws IOException
	 */
	private Tag actionMenu() throws IOException {
		Tag actionMenu = new Tag("div").clazz("actions").content(t("Actions"));		
		Tag actions = new Tag("div").clazz("list").content("");
		new Div(ACTION_POWER).clazz(REALM_CU).content(t("Toggle power")).addTo(actions);
		new Div(ACTION_SAVE).clazz(REALM_PLAN).content(t("Save plan")).addTo(actions);
		new Div(ACTION_ANALYZE).clazz(REALM_PLAN).content(t("Analyze plan")).addTo(actions);
		new Div(ACTION_QR).clazz(REALM_PLAN).content(t("QR-Code")).addTo(actions);
		return actions.addTo(actionMenu);
	}
	
	/**
	 * attaches a new client to the event stream of the plan
	 * @param client
	 */
	public void addClient(OutputStreamWriter client) {
		LOG.debug("Client connected.");
		clients.put(client, 0);
	}
	
	/**
	 * helper function: creates a list element with a link that will call the <it>clickTile</it> function of the client side javascript.
	 * @param tile the tile a click on which shall be simulated
	 * @param content the text to be displayed to the user
	 * @param list the tag to which the link tag shall be added
	 * @return returns the list element itself
	 * TODO: replace occurences by calls to <it>return request({...});</li>, then remove clickTile from the client javascript
	 */
	public static Tag addLink(Tile tile,String content,Tag list) {
		Tag li = new Tag("li");
		new Tag("span").clazz("link").attr("onclick", "return clickTile("+tile.x+","+tile.y+");").content(content).addTo(li).addTo(list);
		return li;
	}
	
	/**
	 * add a tile of the specified class to the track layout
	 * @param clazz
	 * @param xs
	 * @param ys
	 * @param configJson
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws IOException
	 */
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
	
	/**
	 * search all possible routes in the plan
	 * @return a string giving information how many routes have been found
	 */
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

	/**
	 * @return the list of blocks known to the plan
	 */
	public Collection<Block> blocks() {
		return blocks;
	}
	
	/**
	 * calls tile.click()
	 * @param tile
	 * @return
	 * @throws IOException
	 */
	private Object click(Tile tile) throws IOException {
		if (tile == null) return null;
		return tile.click();
	}
	
	/**
	 * @return the control unit currently connected to the plan
	 */
	public ControlUnit controlUnit() {
		return controlUnit;
	}

	/**
	 * completes a given route during a call to {@link #analyze()}.
	 * It therefore traces where the current part of the route comes from and where it may go.
	 * @param route an incomplete route, that shall be completed
	 * @param connector
	 * @return the set of routes, that result from the tracing operation
	 */
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
	
	/**
	 * returns the tile referenced by the tile id
	 * @param tileId a combination of the coordinates of the requested tile
	 * @param resolveShadows if this is set to true, this function will return the overlaying tiles, if the id belongs to a shadow tile.
	 * @return the tile belonging to the id, or the overlaying tile if the respective tile is a shadow tile.
	 */
	public Tile get(String tileId,boolean resolveShadows) {
		Tile tile = tiles.get(tileId);
		if (resolveShadows && tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		return tile;
	}
	
	
	/**
	 * generates the hardware menu attached to the plan
	 * @return
	 * @throws IOException
	 */
	private Tag hardwareMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("hardware").content(t("Hardware"));
		Tag list = new Tag("div").clazz("list").content("");
		new Div(ACTION_PROPS).clazz(REALM_CU).content(t("Control unit")).addTo(list);
		return list.addTo(tileMenu);
	}
	
	/**
	 * prepares the hardware div of the plan
	 * @return
	 */
	private Tag heartbeat() {
		return new Div("heartbeat").content("");
	}

	/**
	 * send a heatbeat to the client
	 */
	public void heatbeat() {
		stream("heartbeat @ "+new Date().getTime());
	}
	
	/**
	 * generates a html document of this plan
	 * @return
	 * @throws IOException
	 */
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
	
	/**
	 * loads a track layout from a file, along with its assigned cars, trains, routes and control unit settings
	 * @param filename
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static Plan load(String filename) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Plan plan = new Plan();
		try {
			Car.loadAll(filename+".cars",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load cars!",e);
		}
		try {
			Train.loadAll(filename+".trains",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load trains!",e);
		}
		Tile.loadAll(filename+".plan",plan);
		try {
			Route.loadAll(filename+".routes",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load routes!",e);
		}
		try {
			plan.controlUnit.load(filename+".cu");			
		} catch (Exception e) {
			LOG.warn("Was not able to load control unit settings!",e);
		}
		try {
			plan.controlUnit.start();
		} catch (Exception e) {
			LOG.warn("Was not able to establish connection to control unit!");
		}
		return plan;
	}
	
	/**
	 * creates the main menu attached to the plan
	 * @return
	 * @throws IOException
	 */
	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		new Tag("div").clazz("emergency").content(t("Emergency")).attr("onclick","return request({realm:'"+REALM_CU+"',action:'"+ACTION_EMERGENCY+"'});").addTo(menu);
		actionMenu().addTo(menu);
		moveMenu().addTo(menu);		
		tileMenu().addTo(menu);
		trainMenu().addTo(menu);
		hardwareMenu().addTo(menu);
		return menu;
	}

	/**
	 * prepares the messages div of the plan
	 * @return
	 */
	private Tag messages() {
		return new Div("messages").content("");
	}
	
	/**
	 * creates the move-tile menu of the plan
	 * @return
	 */
	private Tag moveMenu() {
		Tag tileMenu = new Tag("div").clazz("move").title(t("Move tiles")).content(t("↹"));		
		Tag tiles = new Tag("div").clazz("list").content("");
		new Div("west").title(t("Move west")).content("↤").addTo(tiles);
		new Div("east").title(t("Move east")).content("↦").addTo(tiles);
		new Div("north").title(t("Move north")).content("↥").addTo(tiles);
		new Div("south").title(t("Move south")).content("↧").addTo(tiles);
		return tiles.addTo(tileMenu);
	}
	
	/**
	 * processes move-tile instructions sent from the client
	 * @param direction
	 * @param tileId
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
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

	/**
	 * processes move-tile instructions sent from the client (subroutine)
	 * @param tile
	 * @param direction
	 * @return
	 * @throws IOException
	 */
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

	/**
	 * processes move-tile instructions sent from the client (subroutine)
	 * @param tile
	 * @param xstep
	 * @param ystep
	 * @return
	 * @throws IOException
	 */
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
	
	/**
	 * adds a new tile to the plan on the client side
	 * @param tile
	 * @return
	 * @throws IOException
	 */
	public Tile place(Tile tile) throws IOException {
		stream("place "+tile.tag(null));
		return tile;
	}

	/**
	 * adds a command to the control unit's command queue
	 * @param command
	 * @return
	 */
	public Command queue(Command command) {
		return controlUnit.queue(command);		
	}

	/**
	 * adds a new route to the plan
	 * @param route
	 * @return
	 */
	Route registerRoute(Route route) {
		for (Tile tile: route.path()) tile.add(route);
		routes.put(route.id(), route);
		return route;
	}
	
	/**
	 * removes a tile from the track layout
	 * @param tile
	 */
	private void remove(Tile tile) {
		removeTile(tile.x,tile.y);
		if (tile instanceof Block) blocks.remove(tile);
		for (int i=1; i<tile.len(); i++) removeTile(tile.x+i, tile.y); // remove shadow tiles
		for (int i=1; i<tile.height(); i++) removeTile(tile.x, tile.y+i); // remove shadow tiles
		if (tile != null) stream("remove "+tile.id());
	}
	
	/**
	 * removes a route from the track layout
	 * @param route
	 */
	public void remove(Route route) {
		for (Tile tile : route.path()) tile.remove(route);
		for (Train train : Train.list()) {
			if (train.route == route) train.route = null;
		}
		routes.remove(route.id());
		stream(t("Removed {}.",route));
	}

	/**
	 * removes a tile from the track layout (subroutine)
	 * @param x
	 * @param y
	 */
	private void removeTile(int x, int y) {
		LOG.debug("removed {} from tile list",tiles.remove(Tile.id(x, y)));
	}
	
	/**
	 * returns a specific route from the list of routes assigned to this plan
	 * @param routeId the id of the route requestd
	 * @return
	 */
	public Route route(int routeId) {
		return routes.get(routeId);
	}
	
	/**
	 * saves the plan to a set of files, along with its cars, tiles, trains, routes and control unit settings
	 * @param name
	 * @return
	 * @throws IOException
	 */
	private String saveTo(String name) throws IOException {
		if (name == null || name.isEmpty()) throw new NullPointerException("Name must not be empty!");
		Car.saveAll(name+".cars");
		Tile.saveAll(tiles,name+".plan");
		Train.saveAll(name+".trains"); // refers to cars, blocks
		Route.saveAll(routes.values(),name+".routes"); // refers to tiles
		controlUnit.save(name+".cu");
		return t("Plan saved as \"{}\".",name);
	}
	
	/**
	 * adds a tile to the plan at a specific position
	 * @param x
	 * @param y
	 * @param tile
	 * @throws IOException
	 */
	public void set(int x,int y,Tile tile) throws IOException {
		if (tile == null) return;
		if (tile instanceof Block) blocks.add((Block) tile);
		for (int i=1; i<tile.len(); i++) set(x+i,y,new Shadow(tile));
		for (int i=1; i<tile.height(); i++) set(x,y+i,new Shadow(tile));
		setIntern(x,y,tile);
		place(tile);		
	}
	
	/**
	 * adds a tile to the plan at a specific position (subroutine)
	 * @param x
	 * @param y
	 * @param tile
	 */
	private void setIntern(int x, int y, Tile tile) {
		tile.position(x, y).plan(this);
		tiles.put(tile.id(),tile);
	}

	/**
	 * shows the properties of an entity specified in the params.context value
	 * @param params
	 * @return
	 */
	public Window showContext(HashMap<String, String> params) {
		String[] parts = params.get(CONTEXT).split(":");
		String realm = parts[0];
		String id = parts.length>1 ? parts[1] : null;
		switch (realm) {
			case REALM_ROUTE:
				return route(Integer.parseInt(id)).properties();
		}
		return null;
	}
	
	/**
	 * sends some data to the clients
	 * @param data
	 */
	public synchronized void stream(String data) {
		data = data.replaceAll("\n", "").replaceAll("\r", "");
		//if (!data.startsWith("heartbeat")) LOG.debug("streaming: {}",data);
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
	
	/**
	 * shorthand for Translations.get(message,fills)
	 * @param message
	 * @param fills
	 * @return
	 */
	private String t(String message, Object...fills) {
		return Translation.get(Application.class, message, fills);
	}
	
	/**
	 * generates the menu for selecting tiles to be added to the layout
	 * @return
	 * @throws IOException
	 */
	private Tag tileMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("addtile").title(t("Add tile")).content("╦");
		
		Tag tiles = new Tag("div").clazz("list").content("");
		new StraightH().tag(null).addTo(tiles);
		new StraightV().tag(null).addTo(tiles);
		new ContactH().tag(null).addTo(tiles);
		new ContactV().tag(null).addTo(tiles);
		new SignalW().tag(null).addTo(tiles);
		new SignalE().tag(null).addTo(tiles);
		new SignalS().tag(null).addTo(tiles);
		new SignalN().tag(null).addTo(tiles);
		new BlockH().tag(null).addTo(tiles);
		new BlockV().tag(null).addTo(tiles);
		new DiagES().tag(null).addTo(tiles);
		new DiagSW().tag(null).addTo(tiles);
		new DiagNE().tag(null).addTo(tiles);
		new DiagWN().tag(null).addTo(tiles);
		new EndE().tag(null).addTo(tiles);
		new EndW().tag(null).addTo(tiles);
		new EndN().tag(null).addTo(tiles);
		new EndS().tag(null).addTo(tiles);
		new TurnoutRS().tag(null).addTo(tiles);
		new TurnoutRN().tag(null).addTo(tiles);
		new TurnoutRW().tag(null).addTo(tiles);
		new TurnoutRE().tag(null).addTo(tiles);
		new TurnoutLN().tag(null).addTo(tiles);
		new TurnoutLS().tag(null).addTo(tiles);
		new TurnoutLW().tag(null).addTo(tiles);
		new TurnoutLE().tag(null).addTo(tiles);
		new Turnout3E().tag(null).addTo(tiles);
		new CrossH().tag(null).addTo(tiles);
		new CrossV().tag(null).addTo(tiles);
		new Eraser().tag(null).addTo(tiles);
		return tiles.addTo(tileMenu);
	}
	
	/**
	 * generates the train menu
	 * @return
	 * @throws IOException
	 */
	private Tag trainMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("trains").content(t("Trains"));		
		Tag tiles = new Tag("div").clazz("list").content("");
		new Div(ACTION_PROPS).clazz(REALM_TRAIN).content(t("Manage trains")).addTo(tiles);
		new Div(ACTION_PROPS).clazz(REALM_LOCO).content(t("Manage locos")).addTo(tiles);
		return tiles.addTo(tileMenu);
	}

	/**
	 * updates a tile
	 * @param tile
	 * @param params
	 * @return
	 * @throws IOException
	 */
	private Tile update(Tile tile, HashMap<String, String> params) throws IOException {
		return tile == null ? null : tile.update(params);
	}

	/**
	 * sends a Ghost train warning to the client
	 * @param contact
	 */
	public void warn(Contact contact) {
		stream(t("Warning: {}",t("Ghost train @ {}",contact)));
	}
}
