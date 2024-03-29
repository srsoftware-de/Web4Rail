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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.localconfig.Configuration;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.devices.Device;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Div;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.ControlUnit;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.BlockContact;
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
import de.srsoftware.web4rail.tiles.DecouplerH;
import de.srsoftware.web4rail.tiles.DecouplerV;
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
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.SignalE;
import de.srsoftware.web4rail.tiles.SignalN;
import de.srsoftware.web4rail.tiles.SignalS;
import de.srsoftware.web4rail.tiles.SignalW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.TextDisplay;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.TileWithShadow;
import de.srsoftware.web4rail.tiles.Turnout;
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
 * @author Stephan Richter, SRSoftware, 2020-2021
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
		
		public Heartbeat() {
			setName(Application.threadName(this));
			start();
		}
		
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
	private static final String CONFIRM = "confirm";
	private static final String FINAL_SPEED = "final_speed"; 
	private static final String FREE_BEHIND_TRAIN = "free_behind_train";
	private static final String RENAME = "rename";
	private static final String SPEED_STEP = "speed_step";
	private static final String ALLOW_JSON_EDIT = "allow_json_edit";
	private static final String DISCOVERY_MODE = "discovery_mode";
	private static final String DISCOVER_NEW = "discover_new";
	private static final String DISCOVER_UPDATE = "discover_update";
	private static final String MAINTENANCE_INTERVAL = "maintenance_interval";

	private String name = DEFAULT_NAME;
	
	private ControlUnit controlUnit = new ControlUnit(this); // the control unit, to which the plan is connected 
	private Contact learningContact;
	private Configuration appConfig;
	private LinkedList<EventListener> listeners = new LinkedList<>();
	public static boolean allowJsonEdit = false;
	
	/**
	 * creates a new plan, starts to send heart beats
	 */
	public Plan() {	
		BaseClass.resetRegistry();
		new Heartbeat();
		name = DEFAULT_NAME;
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
	public Object action(Params params) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		switch (params.getString(ACTION)) {
		case ACTION_ADD:
			return addTile(params.getString(TILE),params.getInt(X),params.getInt(Y),null);
		case Block.ACTION_ADD_CONTACT:
			Block block = get(Id.from(params));
			return block.addContact();
		case ACTION_ANALYZE:	
			return analyze(params);
		case ACTION_AUTO:
			return simplifyRouteName(params);
		case ACTION_CLICK:
			return click(get(Id.from(params),true),params.getString("shift"));
		case ACTION_CONNECT:
			Tile tile = get(Id.from(params), false);
			if (tile instanceof Bridge) return ((Bridge)tile).requestConnect();
			break;
		case ACTION_FREE:
			Tile t = get(Id.from(params), false);
			t.free(t.lockingTrain());
			plan.alter();
			return t.properties();
		case ACTION_MOVE:
			return moveTile(params.getString(DIRECTION),Id.from(params));
		case ACTION_PROPS:
			return properties(params);
		case ACTION_POWER:
			Signal signal = get(Id.from(params));
			if (isSet(signal)) {
				signal.state(params.getString(Signal.STATE));
				return signal.properties();
			}
			return null;
		case RENAME:
			return rename(params);
		case ACTION_SAVE:
			return save();
		case ACTION_TIMES:
			return updateTimes(params);
		case ACTION_UPDATE:
			return update(params);
		}
		return t("Unknown action: {}",params.getString(ACTION));
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
	private String addTile(String clazz, int x, int y, String configJson) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException {
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
		tile.position(x, y);
		if (tile instanceof TileWithShadow) ((TileWithShadow)tile).placeShadows();
		tile.parent(this);
		place(tile);
		return t("Added {}",tile.getClass().getSimpleName());
	}
	
	public void alter() {
		while (!listeners.isEmpty()) listeners.removeFirst().fire();
	}
	
	/**
	 * search all possible routes in the plan
	 * @param params 
	 * @return a string giving information how many routes have been found
	 */
	private Object analyze(Params params) {
		List<Route> oldRoutes = BaseClass.listElements(Route.class);

		if (!oldRoutes.isEmpty() && !"yes".equals(params.get(CONFIRM))) {
			Window win = new Window("confirm-analyze", t("Confirmation required"));
			new Tag("p").content(t("Your plan currently has {} routes.",oldRoutes.size())).addTo(win);
			new Tag("p").content(t("Analyze may overwrite these routes!")).addTo(win);
			
			Form form = new Form("plan-analyze-form");
			
			Tag p = new Tag("p");
			new Input(REALM,REALM_PLAN).hideIn(form);
			new Input(ACTION,ACTION_ANALYZE).hideIn(form);
			new Input(CONFIRM,"yes").hideIn(form);
			new Radio(DISCOVERY_MODE, DISCOVER_UPDATE, t("Search new routes, update existing"), true).addTo(p);
			new Radio(DISCOVERY_MODE, DISCOVER_NEW, t("Search new routes, do not update existing"), false).addTo(p);
			p.addTo(form);
			
			new Button(t("analyze"),form).addTo(form);
			button(t("abort")).addTo(form);

			form.addTo(win);
			return win;
		}
		
		boolean keepExisting = DISCOVER_NEW.equals(params.getString(DISCOVERY_MODE));
		
		new Thread(Application.threadName("Plan.Analyzer")) {
			public void run() {				
				Vector<Route> newRoutes = new Vector<Route>();
				for (Block block : BaseClass.listElements(Block.class)) {
					for (Connector con : block.startPoints()) newRoutes.addAll(follow(new Route().begin(block,con.from.inverse()),con));
				}
				for (Tile tile : BaseClass.listElements(Tile.class)) tile.routes().clear();
				int count = 0;
				for (Route newRoute : newRoutes) {
					newRoute.complete();
					Route replacedRoute = BaseClass.get(newRoute.id());
					if (isSet(replacedRoute)) {
						if (keepExisting) continue;
						newRoute.addPropertiesFrom(replacedRoute);					
					}
					registerRoute(newRoute);
					count ++;
				}
				for (Route oldRoute : oldRoutes) {
					if (keepExisting) {
						registerRoute(oldRoute);
					} else {
						oldRoute.id = new Id("test"); // new routes may have the same ids and shall not be deleted in the next step!
						oldRoute.remove();
					}
				}
				
				stream(t(keepExisting?"Added {} routes.":"Found {} routes.",count));
			}		
		}.start();
		
		return t("Analyzing plan...");
	}

	/**
	 * calls tile.click()
	 * @param tile
	 * @return
	 * @throws IOException
	 */
	private Object click(Tile tile,String shift) throws IOException {
		if (tile == null) return null;
		return tile.click("1".equals(shift));
	}
	
	/**
	 * @return the control unit currently connected to the plan
	 */
	public ControlUnit controlUnit() {
		return controlUnit;
	}
	
	public void drop(Tile tile) {
		tile.unregister();
		stream("remove "+tile.id());		
	}
	
	
	Fieldset editableProperties() {
		Fieldset fieldset = new Fieldset(t("Editable properties"));
		//new Tag("h4").content(t("Editable properties")).addTo(win);
		Form form = new Form("plan-properties-form");
		new Input(REALM,REALM_PLAN).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(LENGTH_UNIT, lengthUnit).addTo(new Label(t("Length unit")+COL)).addTo(form);
		new Input(SPEED_UNIT, speedUnit).addTo(new Label(t("Speed unit")+COL)).addTo(form);
		new Input(FINAL_SPEED, Train.defaultEndSpeed).addTo(new Label(t("Lower speed limit")+COL)).attr("title", t("Final speed after breaking, before halting")).addTo(form);
		new Checkbox(FREE_BEHIND_TRAIN, t("Free tiles behind train"), Route.freeBehindTrain).attr("title", t("If checked, tiles behind the train are freed according to the length of the train and the tiles. If it is unchecked, tiles will not get free before route is finished.")).addTo(form);
		new Button(t("Save"), form).addTo(form);
		form.addTo(fieldset);
		return fieldset;
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
		if (addedTile instanceof Block) { // Route wird mit einem Block abgeschlossen
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
			if (tile instanceof BlockContact) continue;
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
	
	public JSONObject json() {
		JSONArray jTiles = new JSONArray();
		BaseClass.listElements(Tile.class)
			.stream()
			.filter(tile -> !(tile instanceof Shadow || tile instanceof BlockContact))
			.map(tile -> tile.json())
			.forEach(jTiles::put);
		
		return new JSONObject()
				.put(FINAL_SPEED, Train.defaultEndSpeed) 
				.put(SPEED_STEP, Train.defaultSpeedStep)
				.put(FREE_BEHIND_TRAIN, Route.freeBehindTrain)
				.put(MAINTENANCE_INTERVAL, Car.defaulMaintenanceDist)
				.put(LENGTH_UNIT, lengthUnit)
				.put(SPEED_UNIT, speedUnit)
				.put(Turnout.DELAY, Turnout.delay)
				.put(TILE, jTiles)
				.put(REALM_LOOKUP, LookupTable.jsonList());
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
	public static void load(String name) throws IOException {
		plan = new Plan();
		plan.name = name;

		String content = new String(Files.readAllBytes(new File(name+".plan").toPath()),UTF8);
		JSONObject json = new JSONObject(content);

		if (json.has(LENGTH_UNIT)) lengthUnit = json.getString(LENGTH_UNIT);
		if (json.has(SPEED_UNIT)) speedUnit = json.getString(SPEED_UNIT);
		if (json.has(FINAL_SPEED)) Train.defaultEndSpeed = json.getInt(FINAL_SPEED);
		if (json.has(SPEED_STEP)) Train.defaultSpeedStep = json.getInt(SPEED_STEP);
		if (json.has(FREE_BEHIND_TRAIN)) Route.freeBehindTrain = json.getBoolean(FREE_BEHIND_TRAIN);
		if (json.has(MAINTENANCE_INTERVAL)) Car.defaulMaintenanceDist = json.getLong(MAINTENANCE_INTERVAL);
		if (json.has(Turnout.DELAY)) Turnout.delay = json.getInt(Turnout.DELAY);
	
		try {
			Car.loadAll(name+".cars",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load cars!",e);
		}

		try {
			Train.loadAll(name+".trains",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load trains!",e);
		}

		if (json.has(TILE)) json.getJSONArray(TILE).forEach(object -> Tile.load(object, plan));
			
		try {
			Route.loadAll(name+".routes",plan);
		} catch (Exception e) {
			LOG.warn("Was not able to load routes!",e);
		}
		try {
			plan.controlUnit.load(name+".cu");			
		} catch (Exception e) {
			LOG.warn("Was not able to load control unit settings!",e);
		}
		try {
			plan.controlUnit.start();
		} catch (Exception e) {
			LOG.warn("Was not able to establish connection to control unit!");
		}
		
		try {
			LookupTable.loadAll(json.getJSONArray(REALM_LOOKUP));
		} catch (Exception e) {
			LOG.warn("Was not able to load lookup tables!");
		}
		
		History.load(name+".history");
		
		LoadCallback.fire();
	}
	
	private Fieldset lookupTables() {
		Fieldset fieldset = new Fieldset(t("lookup tables"));
		Button button = button(t("add"),Map.of(REALM,REALM_LOOKUP,ACTION,ACTION_ADD));
		button.addTo(fieldset);
		List<LookupTable> tables = BaseClass.listElements(LookupTable.class);

		if (!tables.isEmpty()) {
			new Tag("div").content("known lookup tables:").addTo(fieldset);
			Tag list = new Tag("ul");
			for (LookupTable table : tables) table.button(table.name()).addTo(list);
			list.addTo(fieldset);
		}
		return fieldset;
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
		return t(moved ? "Tile(s) moved.":"No tile moved.");
	}
	
	public void onChange(EventListener listener) {
		listeners.add(listener);
	}

	/**
	 * adds a new tile to the plan on the client side
	 * @param tile
	 * @return
	 * @throws IOException
	 */
	public Tile place(Tile tile) {
		try {
//			tile.parent(this);
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
		saveButton().addTo(actions);
		new Div(ACTION_ANALYZE).clazz(REALM_PLAN).content(t("Analyze")).addTo(actions);
		new Div(ACTION_QR).clazz(REALM_PLAN).content(t("QR-Code")).addTo(actions);
		new Div(FULLSCREEN).clazz(REALM_PLAN).content(t("Fullscreen")).addTo(actions);
		new Div(ACTION_PROPS).clazz(REALM_PLAN).content(t("Properties")).addTo(actions);
		new Div(RENAME).clazz(REALM_PLAN).content(t("rename")).addTo(actions);
		new Div(ACTION_OPEN).clazz(REALM_APP).content(t("open other plan")).addTo(actions);
		return actions.addTo(actionMenu);
	}
	
	public Window properties(Params params) {
		
		if (params.containsKey(ID)) {
			Tile tile = get(Id.from(params), true);
			if (isSet(tile)) return tile.properties();
		}		
		
		return properties();
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm, String... errorMessages) {
		formInputs.add(null, new Input(REALM,REALM_PLAN));
		formInputs.add(null, new Input(ACTION,ACTION_UPDATE));
		formInputs.add(t("Length unit"),new Input(LENGTH_UNIT, lengthUnit));
		formInputs.add(t("Speed unit"),new Input(SPEED_UNIT, speedUnit));
		formInputs.add(t("Speed step"),new Input(SPEED_STEP, Train.defaultSpeedStep).attr("title", t("Speeds are always increadsed/decreased by this value")).addTo(new Tag("span")).content(NBSP+Plan.speedUnit));
		formInputs.add(t("Lower speed limit"),new Input(FINAL_SPEED, Train.defaultEndSpeed).attr("title", t("Final speed after breaking, before halting")).addTo(new Tag("span")).content(NBSP+Plan.speedUnit));
		formInputs.add(t("Default maintenance intervall"),new Input(MAINTENANCE_INTERVAL, Car.defaulMaintenanceDist).numeric().addTo(new Tag("span")).content(NBSP+Plan.lengthUnit));
		formInputs.add(t("Free tiles behind train"),new Checkbox(FREE_BEHIND_TRAIN, t("If checked, tiles behind the train are freed according to the length of the train and the tiles. If it is unchecked, tiles will not get free before route is finished."), Route.freeBehindTrain));
		formInputs.add(t("Allow editing JSON of action lists"),new Checkbox(ALLOW_JSON_EDIT, t("Do you know, what you are doing?"), allowJsonEdit ));
		formInputs.add(t("Pause between switching turnouts of route"),new Input(Turnout.DELAY, Turnout.delay).numeric());
		
		postForm.add(relayProperties());
		postForm.add(routeProperties());
		postForm.add(lookupTables());
		return super.properties(preForm, formInputs, postForm, errorMessages);
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
	 * adds a route to the plan
	 * @param route
	 * @return
	 */
	Route registerRoute(Route route) {
		route.path().stream().filter(Tile::isSet).forEach(tile -> tile.add(route));
		route.parent(this).register();
		return route;
	}
	
	private Fieldset relayProperties() {
		Fieldset fieldset = new Fieldset(t("Accessory"));
		Table table = new Table();
		table.addHead(t("Address"),t("Relay/Signal/Turnout"));
		
		List<Device> devices = BaseClass.listElements(Tile.class)
			.stream()
			.filter(tile -> tile instanceof Device )
			.map(tile -> (Device) tile)
			.collect(Collectors.toList());

		for (Signal signal : BaseClass.listElements(Signal.class)) {
			for (int addr : signal.addresses()) {
				devices.add(new Device() {					
					@Override
					public int address() {
						return addr;
					}

					@Override
					public Tag link(String... args) {
						return signal.link(args);
					}
					
					@Override
					public String toString() {
						return signal.toString();
					}
				});
			}
		}

		Collections.sort(devices, (d1,d2) -> d1.address() - d2.address());

		for (Device device : devices) {
			table.addRow(device.address(),device.link(device.toString()));
			if (device.address() % 4 == 1) table.children().lastElement().clazz("group");
			
		}
		return table.clazz("turnouts").addTo(fieldset);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child instanceof Tile) drop((Tile) child);
		super.removeChild(child);
	}
	
	private Object rename(Params params) {
		String newName = params.getString(NAME);
		Window win = new Window("rename-plan", t("Rename plan"));
		if (isSet(newName)) {
			newName = newName.trim();
			if (!newName.isEmpty() && !newName.equals(name)) {
				String old = name;
				name = newName;				
				try {
					String saved = save();
					appConfig.put(NAME, name);
					appConfig.save();
					stream("place "+saveButton());
					return saved;
				} catch (IOException e) {
					new Tag("div").content(t("Was not able to save plan as \"{}\".",newName));
					name = old;
				}
			}
		}
		Form form = new Form("rename-form");
		new Input(REALM, REALM_PLAN).hideIn(form);
		new Input(ACTION, RENAME).hideIn(form);
		new Input(NAME,name).addTo(new Label(t("Enter new name for plan")+COL)).addTo(form);
		new Button(t("Save"), form).addTo(form);
		return form.addTo(win);
	}


	private Fieldset routeProperties() {
		Fieldset fieldset = new Fieldset(t("Routes"));
		Table table = new Table();
		table.addHead(t("Name"),t("Start"),t("End"),t("Actions"));
		List<Route> routes = BaseClass.listElements(Route.class);
		Collections.sort(routes, (r1,r2) -> r1.name().compareTo(r2.name()));
		for (Route route : routes) {
			Tag actions = new Tag("div");
			plan.button(t("simplify name"), Map.of(ACTION,ACTION_AUTO,ROUTE,route.id().toString())).addTo(actions);
			route.button(t("delete"), Map.of(ACTION,ACTION_DROP)).addTo(actions);
			Tag row = table.addRow(
					route.link("span",route.name()),
					route.link("span", route.startBlock()),
					route.link("span", route.endBlock()),
					actions);
			if (route.isDisabled())	row.clazz("disabled");
		}
		table.clazz("turnouts").addTo(fieldset);
		return button(t("simplify all names"), Map.of(REALM,REALM_ROUTE,ACTION,ACTION_AUTO,ID,"*")).addTo(fieldset);
	}
	
	/**
	 * saves the plan to a set of files, along with its cars, tiles, trains, routes and control unit settings
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public String save() throws IOException {
		if (isNull(name) || name.isEmpty()) throw new NullPointerException("Name must not be empty!");
		Car.saveAll(name+".cars");		
		
		Tile.saveAll(name+".plan");
		Train.saveAll(name+".trains"); // refers to cars, blocks		
		Route.saveAll(name+".routes"); // refers to tiles
		controlUnit.save(name+".cu");
		
		BufferedWriter file = new BufferedWriter(new FileWriter(name+".plan"));
		file.write(json().toString());
		file.close();
		
		History.save(name+".history");
		
		return t("Plan saved as \"{}\".",name);
	}
	
	private Tag saveButton() {
		return new Div(ACTION_SAVE).clazz(REALM_PLAN).content(t("Save \"{}\"",name));
	}

	public void sensor(int addr, boolean active) {
		Contact contact = Contact.get(addr);
		if (active && isSet(learningContact)) learnContact(addr,contact);
		if (isSet(contact)) contact.activate(active);
	}
	
	private void learnContact(int addr, Contact contact) {
		if (isSet(contact)) {
			contact.addr(0);
			LOG.debug("unsibscribed {} from {}",contact,addr);
		}
		
		stream(learningContact.addr(addr).properties().toString());
		LOG.debug("learned: {} = {}",addr,learningContact);			
		learningContact = null;
		return;
	}

	public void setAppConfig(Configuration config) {
		appConfig = config;
	}

	
	private Object simplifyRouteName(Params params) {
		String routeId = params.getString(ROUTE);
		if (isSet(routeId)) {
			Route route = BaseClass.get(new Id(routeId));
			if (isSet(route)) route.simplyfyName();
		}
		Id tileId = Id.from(params);
		Tile tile = isSet(tileId)? BaseClass.get(tileId) : null;
		if (isSet(tile)) return tile.properties();
		params.remove(ID);
		return plan.properties(params);
	}
	
	/**
	 * sends some data to the clients
	 * @param data
	 */
	public synchronized void stream(String data) {
		String fixedData = data.replaceAll("\n", "%newline%").replaceAll("\r", ""); 
		new Thread("Plan") {
			@Override
			public void run() {
				//if (!data.startsWith("heartbeat")) LOG.debug("streaming: {}",data);
				Vector<OutputStreamWriter> badClients = null;
				for (Entry<OutputStreamWriter, Integer> entry : clients.entrySet()) {
					OutputStreamWriter client = entry.getKey();
					try {
						client.write("data: "+fixedData+"\n\n");
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
				// TODO Auto-generated method stub
			}
		}.start();

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
		new DecouplerH().tag(null).addTo(tiles);
		new DecouplerV().tag(null).addTo(tiles);
		new Relay().setLabel(true,"RL").tag(null).addTo(tiles);
		new Contact().tag(null).addTo(tiles);
		new Switch().tag(null).addTo(tiles);
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
	
	@Override
	public String toString() {
		return name;
	}

	/**
	 * updates a tile
	 * @param tile
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public Object update(Params params) {
		super.update(params);
		Tile tile = get(Id.from(params),true);
		if (isSet(tile)) return tile.update(params).properties();
		
		if (params.containsKey(LENGTH_UNIT)) lengthUnit = params.getString(LENGTH_UNIT);
		if (params.containsKey(SPEED_UNIT)) speedUnit = params.getString(SPEED_UNIT);
		if (params.containsKey(SPEED_STEP)) Train.defaultSpeedStep = params.getInt(SPEED_STEP);
		if (params.containsKey(FINAL_SPEED)) Train.defaultEndSpeed = params.getInt(FINAL_SPEED);
		if (params.containsKey(Turnout.DELAY)) Turnout.delay = params.getInt(Turnout.DELAY);
		if (params.containsKey(MAINTENANCE_INTERVAL)) try {
			Car.defaulMaintenanceDist = params.getLong(MAINTENANCE_INTERVAL);
		} catch(NumberFormatException e) {};
		allowJsonEdit = "on".equalsIgnoreCase(params.getString(ALLOW_JSON_EDIT));
		Route.freeBehindTrain = "on".equalsIgnoreCase(params.getString(FREE_BEHIND_TRAIN));
		
		return properties(t("Plan updated."));		
	}
	
	private Object updateTimes(Params params) throws IOException {
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
		//stream(t("Warning: {}",t("Ghost train @ {}",contact)));
	}
}
