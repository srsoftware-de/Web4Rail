package de.srsoftware.web4rail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Div;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.BlockH;
import de.srsoftware.web4rail.tiles.BlockV;
import de.srsoftware.web4rail.tiles.Bridge;
import de.srsoftware.web4rail.tiles.BridgeE;
import de.srsoftware.web4rail.tiles.BridgeN;
import de.srsoftware.web4rail.tiles.BridgeS;
import de.srsoftware.web4rail.tiles.BridgeW;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.ContactH;
import de.srsoftware.web4rail.tiles.ContactV;
import de.srsoftware.web4rail.tiles.CrossH;
import de.srsoftware.web4rail.tiles.CrossPlus;
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
import de.srsoftware.web4rail.tiles.Relay;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.SignalE;
import de.srsoftware.web4rail.tiles.SignalN;
import de.srsoftware.web4rail.tiles.SignalS;
import de.srsoftware.web4rail.tiles.SignalW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.StretchableTile;
import de.srsoftware.web4rail.tiles.TextDisplay;
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
public class Plan extends BaseClass{
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
	

	private static final String ACTION_QR = "qrcode";
	public  static final String DEFAULT_NAME = "default";
	private static final String DIRECTION = "direction";
	private static final Logger LOG = LoggerFactory.getLogger(Plan.class);
	private static final String TILE = "tile";
	private static final String X = "x";
	private static final String Y = "y";
	private static final HashMap<OutputStreamWriter,Integer> clients = new HashMap<OutputStreamWriter, Integer>();
	private static final String FULLSCREEN = "fullscreen";
	private static final String SPEED_UNIT = "speed_unit";
	private static final String LENGTH_UNIT = "length_unit";
	
	private ControlUnit controlUnit = new ControlUnit(this); // the control unit, to which the plan is connected 
	private Contact learningContact;
	
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
			return click(get(Id.from(params),true));
		case ACTION_CONNECT:
			Tile tile = get(Id.from(params), false);
			if (tile instanceof Bridge) return ((Bridge)tile).requestConnect();
			break;
		case ACTION_MOVE:
			return moveTile(params.get(DIRECTION),Id.from(params));
		case ACTION_PROPS:
			return properties(params);
		case ACTION_SAVE:
			return saveTo(DEFAULT_NAME);
		case ACTION_TIMES:
			return updateTimes(params);
		case ACTION_UPDATE:
			return update(params);
		}
		return t("Unknown action: {}",params.get(ACTION));
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
			if (isSet(erased)) {
				erased.remove();
				return t("Removed {}.",erased);
			}
			return null;
		}
		if (tile instanceof StretchableTile) ((StretchableTile)tile).placeShadows();
		place(tile.position(x, y));
		return t("Added {}",tile.getClass().getSimpleName());
	}
	
	/**
	 * search all possible routes in the plan
	 * @return a string giving information how many routes have been found
	 */
	private String analyze() {
		List<Route> oldRoutes = BaseClass.listElements(Route.class);
		Vector<Route> newRoutes = new Vector<Route>();
		for (Block block : BaseClass.listElements(Block.class)) {
			if (block.name.equals("Huhu")) {
				System.err.println("Here we go!");
			}
			for (Connector con : block.startPoints()) {
				newRoutes.addAll(follow(new Route().begin(block,con.from.inverse()),con));
			}
		}
		for (Tile tile : BaseClass.listElements(Tile.class)) tile.routes().clear();
		for (Route route : newRoutes) registerRoute(route.complete());
		for (Route oldRoute : oldRoutes) {
			oldRoute.id = new Id("test"); // new routes may have the same ids and shall not be deleted in the next step!
			oldRoute.remove();
		}
		return t("Found {} routes.",newRoutes.size());
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
		Tile addedTile = route.add(tile,connector.from.inverse());
		if (addedTile instanceof Block) {
			Map<Connector, State> cons = addedTile.connections(connector.from);
			LOG.debug("Found {}, coming from {}.",addedTile,connector.from);
			for (Connector con : cons.keySet()) { // falls direkt nach dem Block noch ein Kontakt kommt: diesen mit zu Route hinzufügen
				LOG.debug("This is connected to {}",con);
				Tile nextTile = get(Tile.id(con.x,con.y),false);
				if (nextTile instanceof Contact) {
					LOG.debug("{} is followed by {}",addedTile,nextTile);
					route.add(nextTile, con.from.inverse());
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
	public Tile get(Id tileId,boolean resolveShadows) {
		if (isNull(tileId)) return null;
		Tile tile = BaseClass.get(tileId);
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
		new Div(ACTION_POWER).clazz(REALM_CU).content(t("Toggle power")).addTo(list);
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
	
	private Tag help() {
		Tag help = new Tag("div").clazz("help").content(t("Help"));
		Tag list = new Tag("div").clazz("list").content("");
		new Tag("div").content(t("Online Documentation")).attr("onclick", "window.open('"+GITHUB_URL+"')").addTo(list);
		new Tag("div").content(t("Report Issue")).attr("onclick", "window.open('"+GITHUB_URL+"/issues')").addTo(list);
		return list.addTo(help);
	}
	
	/**
	 * generates a html document of this plan
	 * @return
	 * @throws IOException
	 */
	public Page html() throws IOException {
		Page page = new Page().append("<div id=\"plan\"><div id=\"scroll\">");
		for (Tile tile: BaseClass.listElements(Tile.class)) {
			if (tile == null) continue;
			page.append("\t\t"+tile.tag(null)+"\n");
		}
		return page
				.append("</div>")
				.append(menu())
				.append(messages())
				.append(heartbeat())
				.append("</div>")
				.style("css/style.css")
				.js("js/jquery-3.5.1.min.js")
				.js("js/plan.js");
	}
	
	public void learn(Contact contact) {
		learningContact = contact;
		LOG.debug("learning contact {}",learningContact);
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
	public static void load(String filename) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		plan = new Plan();
		try {
			Car.loadAll(filename+".cars",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load cars!",e);
		}

		String content = new String(Files.readAllBytes(new File(filename+".plan").toPath()),UTF8);
		JSONObject json = new JSONObject(content);
		if (json.has(TILE)) json.getJSONArray(TILE).forEach(object -> Tile.load(object, plan));
		if (json.has(LENGTH_UNIT)) lengthUnit = json.getString(LENGTH_UNIT);
		if (json.has(SPEED_UNIT)) speedUnit = json.getString(SPEED_UNIT);
			
		try {
			Train.loadAll(filename+".trains",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load trains!",e);
		}
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
	}
	
	/**
	 * creates the main menu attached to the plan
	 * @return
	 * @throws IOException
	 */
	private Tag menu() throws IOException {
		Tag menu = new Tag("div").clazz("menu");
		new Tag("div").clazz("emergency").content(t("Emergency")).attr("onclick","return request({realm:'"+REALM_CU+"',action:'"+ACTION_EMERGENCY+"'});").addTo(menu);
		moveMenu().addTo(menu);		
		planMenu().addTo(menu);
		hardwareMenu().addTo(menu);
		tileMenu().addTo(menu);
		trainMenu().addTo(menu);
		help().addTo(menu);
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
	private String moveTile(String direction, Id tileId) throws NumberFormatException, IOException {
		switch (direction) {
		case "south":
			return moveTile(get(tileId,true),Direction.SOUTH);
		case "north":
			return moveTile(get(tileId,true),Direction.NORTH);
		case "east":
			return moveTile(get(tileId,true),Direction.EAST);
		case "west":
			return moveTile(get(tileId,true),Direction.WEST);
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
					moved = tile.move(+1,0);
					break;
				case WEST:
					moved = tile.move(-1,0);
					break;
				case NORTH:
					moved = tile.move(0,-1);
					break;
				case SOUTH:
					moved = tile.move(0,+1);
					break;
			}
		}
		return t(moved ? "Tile(s) moved.":"No tile(s) moved.");
	}

	public void drop(Tile tile) {
		tile.unregister();
		stream("remove "+tile.id());		
	}

	/**
	 * adds a new tile to the plan on the client side
	 * @param tile
	 * @return
	 * @throws IOException
	 */
	public Tile place(Tile tile) {
		try {
			tile.parent(this);
			tile.register();
			stream("place "+tile.tag(null));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tile;
	}
	
	/**
	 * generates the action menu that is appended to the plan
	 * @return
	 * @throws IOException
	 */
	private Tag planMenu() throws IOException {
		Tag actionMenu = new Tag("div").clazz("actions").content(t("Plan"));		
		Tag actions = new Tag("div").clazz("list").content("");
		new Div(ACTION_SAVE).clazz(REALM_PLAN).content(t("Save")).addTo(actions);
		new Div(ACTION_ANALYZE).clazz(REALM_PLAN).content(t("Analyze")).addTo(actions);
		new Div(ACTION_QR).clazz(REALM_PLAN).content(t("QR-Code")).addTo(actions);
		new Div(FULLSCREEN).clazz(REALM_PLAN).content(t("Fullscreen")).addTo(actions);
		new Div(ACTION_PROPS).clazz(REALM_PLAN).content(t("Properties")).addTo(actions);
		return actions.addTo(actionMenu);
	}
	
	private Window properties(HashMap<String, String> params) {
		if (params.containsKey(ID)) {
			Tile tile = get(Id.from(params), true);
			if (isSet(tile)) return tile.properties();
		}
		
		Window win = new Window("plan-properties", t("Properties"));
		
		new Tag("h4").content(t("Editable properties")).addTo(win);
		Form form = new Form("plan-properties-form");
		new Input(REALM,REALM_PLAN).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(LENGTH_UNIT, lengthUnit).addTo(new Label(t("Length unit")+":"+NBSP)).addTo(form);
		new Input(SPEED_UNIT, speedUnit).addTo(new Label(t("Speed unit")+":"+NBSP)).addTo(form);
		new Button(t("Save"), form).addTo(form);
		form.addTo(win);
		
		new Tag("h4").content(t("turnout properties")).addTo(win);
		Table table = new Table();
		table.addHead(t("Address"),t("Relay/Turnout"));
		BaseClass.listElements(Tile.class)
			.stream()
			.filter(tile -> tile instanceof Device )
			.map(tile -> (Device) tile)
			.sorted(Comparator.comparing(Device::address))
			.forEach(turnout -> {
				table.addRow(turnout.address(),turnout);
				if (turnout.address() % 4 == 1) table.children().lastElement().clazz("group");
			});
		table.clazz("turnouts").addTo(win);
		
		return win;
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
	 * @param newRoute
	 * @return
	 */
	Route registerRoute(Route newRoute) {
		newRoute.path().stream().filter(Tile::isSet).forEach(tile -> tile.add(newRoute));
		Route existingRoute = BaseClass.get(newRoute.id());
		if (isSet(existingRoute)) newRoute.addPropertiesFrom(existingRoute);
		newRoute.parent(this).register();
		return newRoute;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child instanceof Tile) drop((Tile) child);
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
		
		Tile.saveAll(name+".plan");
		Train.saveAll(name+".trains"); // refers to cars, blocks		
		Route.saveAll(name+".routes"); // refers to tiles
		controlUnit.save(name+".cu");
		
		BufferedWriter file = new BufferedWriter(new FileWriter(name+".plan"));
		file.write(json().toString());
		file.close();
		
		return t("Plan saved as \"{}\".",name);
	}
	
	public JSONObject json() {
		JSONArray jTiles = new JSONArray();
		BaseClass.listElements(Tile.class)
			.stream()
			.filter(tile -> !(tile instanceof Shadow))
			.map(tile -> tile.json())
			.forEach(jTiles::put);
		
		return new JSONObject()
				.put(TILE, jTiles)
				.put(SPEED_UNIT, speedUnit)
				.put(LENGTH_UNIT, lengthUnit);
	}

	public void sensor(int addr, boolean active) {
		Contact contact = Contact.get(addr);
		if (active && learningContact != null) {
			if (isSet(contact)) {
				contact.addr(0);
				LOG.debug("unsibscribed {} from {}",contact,addr);
			}
			stream(learningContact.addr(addr).properties().toString());
			learningContact = null;
			LOG.debug("learned: {} = {}",addr,learningContact);			
			return;
		}
		
		if (isSet(contact)) contact.activate(active);
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
					if (isNull(badClients)) badClients = new Vector<OutputStreamWriter>();
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
	 * generates the menu for selecting tiles to be added to the layout
	 * @return
	 * @throws IOException
	 */
	private Tag tileMenu() throws IOException {
		Tag tileMenu = new Tag("div").clazz("addtile").title(t("Add tile")).content("╦");
		
		Tag tiles = new Tag("div").clazz("list").content("");
		new CrossV().tag(null).addTo(tiles);
		new CrossH().tag(null).addTo(tiles);
		new CrossPlus().tag(null).addTo(tiles);
		new BridgeE().tag(null).addTo(tiles);
		new BridgeN().tag(null).addTo(tiles);
		new BridgeS().tag(null).addTo(tiles);
		new BridgeW().tag(null).addTo(tiles);
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
		new Relay().setLabel(true,"RL").tag(null).addTo(tiles);
		new Contact().tag(null).addTo(tiles);
		new TextDisplay().text("tx").tag(null).addTo(tiles);
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
		new Div(ACTION_PROPS).clazz(REALM_CAR).content(t("Manage cars")).addTo(tiles);
		return tiles.addTo(tileMenu);
	}

	/**
	 * updates a tile
	 * @param tile
	 * @param params
	 * @return
	 * @throws IOException
	 */
	protected Object update(HashMap<String, String> params) {
		super.update(params);
		Tile tile = get(Id.from(params),true);
		if (isSet(tile)) return tile.update(params);
		
		if (params.containsKey(LENGTH_UNIT)) lengthUnit = params.get(LENGTH_UNIT);
		if (params.containsKey(SPEED_UNIT)) speedUnit = params.get(SPEED_UNIT);
		
		return t("Plan updated.");
		
	}
	
	private Object updateTimes(HashMap<String, String> params) throws IOException {
		Tile tile = get(Id.from(params),false);
		if (tile instanceof Block) {
			Block block = (Block) tile;
			place(block.updateTimes(params));
			return tile.properties();
		}
		return t("updateTimes called on non-block tile!");
	}

	/**
	 * sends a Ghost train warning to the client
	 * @param contact
	 */
	public void warn(Contact contact) {
		stream(t("Warning: {}",t("Ghost train @ {}",contact)));
	}
}
