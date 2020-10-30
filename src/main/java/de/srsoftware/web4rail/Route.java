package de.srsoftware.web4rail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.actions.ActivateRoute;
import de.srsoftware.web4rail.actions.FinishRoute;
import de.srsoftware.web4rail.actions.SetSignalsToStop;
import de.srsoftware.web4rail.actions.SetSpeed;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

/**
 * A route is a vector of tiles that leads from one block to another.
 * 
 * @author Stephan Richter, SRSoftware
 *
 */
public class Route implements Constants{
	private static final Logger LOG = LoggerFactory.getLogger(Route.class);
	static final String NAME = "name";
	static final String PATH = "path";
	static final String SIGNALS = "signals";
	static final String TURNOUTS = "turnouts";
	private Vector<Tile> path;
	private Vector<Signal> signals;
	private Vector<Contact> contacts;
	private HashMap<Turnout,Turnout.State> turnouts;
	private HashMap<String,ActionList> triggers = new HashMap<String, ActionList>();
	private int id;
	private static HashMap<Integer, String> names = new HashMap<Integer, String>(); // maps id to name. needed to keep names during plan.analyze()
	public Train train;
	private Block startBlock = null,endBlock;
	private static final String START_DIRECTION = "direction_start";
	private static final String END_DIRECTION = "direction_end";
	
	public Direction startDirection;
	private Direction endDirection;

	private static final String TRIGGER = "trigger";
	private static final String ACTIONS = "actions";
	private static final String ACTION_LISTS = "action_lists";
	
	/**
	 * process commands from the client
	 * @param params
	 * @return
	 * @throws IOException 
	 */
	public static Object action(HashMap<String, String> params,Plan plan) throws IOException {
		Route route = plan.route(Integer.parseInt(params.get(ID)));
		if (route == null) return t("Unknown route: {}",params.get(ID));
		switch (params.get(ACTION)) {
			case ACTION_PROPS:
				return route.properties();
			case ACTION_UPDATE:
				route.update(params);
				return plan.html();
		}
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	/**
	 * Route wurde von Zug betreten
	 * @throws IOException 
	 */
	public void activate() throws IOException {
		LOG.debug("{} aktiviert.",this);
		for (Tile tile : path) tile.train(train);
	}
	
	/**
	 * adds a tile to the route
	 * @param tile
	 * @param direrction
	 * @return
	 */
	public Tile add(Tile tile, Direction direrction) {
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		if (tile instanceof Block) {
			endBlock = (Block) tile;
			endDirection = direrction;
		}
		path.add(tile);
		if (tile instanceof Contact) contacts.add((Contact) tile);
		if (tile instanceof Signal) {
			Signal signal = (Signal) tile;
			if (signal.isAffectedFrom(direrction)) addSignal(signal);			
		}

		return tile;
	}	
	
	/**
	 * adds a action to the action list of the given trigger
	 * @param trigger
	 * @param action
	 */
	public void add(String trigger, Action action) {
		ActionList actions = triggers.get(trigger);
		if (actions == null) {
			actions = new ActionList();
			triggers.put(trigger, actions);
		}
		actions.add(action);
	}
	
	private void addBasicPropertiesTo(Window win) {
		new Tag("h4").content(t("Origin and destination")).addTo(win);
		Tag list = new Tag("ul");
		Plan.addLink(startBlock, t("Origin: {} to {}",startBlock.name,startDirection), list);
		Plan.addLink(endBlock, t("Destination: {} from {}",endBlock.name,endDirection), list);
		list.addTo(win);

		if (!signals.isEmpty()) {
			new Tag("h4").content(t("Signals")).addTo(win);
			list = new Tag("ul");
			for (Signal s : signals) Plan.addLink(s,s.toString(),list);
			list.addTo(win);
		}
	}
	
	private void addContactsTo(Window win) {
		if (!contacts.isEmpty()) {
			new Tag("h4").content(t("Contacts and actions")).addTo(win);
			Tag list = new Tag("ol");
			for (Contact c : contacts) {
				Tag link = Plan.addLink(c,c.toString(),list);
				ActionList actions = triggers.get(c.trigger());
				if (actions == null) {
					actions = new ActionList();
					triggers.put(c.trigger(), actions);
				}
				actions.addTo(link,REALM_ROUTE+":"+id());
			}
			list.addTo(win);
		}
	}
	
	private void addFormTo(Window win) {
		Form form = new Form();
		new Input(ACTION, ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_ROUTE).hideIn(form);
		new Input(ID,id()).hideIn(form);
		
		Tag label = new Tag("label").content(t("name:")+NBSP);
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name()).style("width: 80%").addTo(label);		
		label.addTo(form);
		
		new Tag("button").attr("type", "submit").content(t("save")).addTo(form);
		form.addTo(win);
	}
	
	void addSignal(Signal signal) {
		signals.add(signal);
	}
	
	void addTurnout(Turnout t, State s) {
		turnouts.put(t, s);
	}

	private void addTurnoutsTo(Window win) {
		if (!turnouts.isEmpty()) {
			new Tag("h4").content(t("Turnouts")).addTo(win);
			Tag list = new Tag("ul");
			for (Entry<Turnout, State> entry : turnouts.entrySet()) {
				Turnout turnout = entry.getKey();
				Plan.addLink(turnout, turnout+": "+entry.getValue(), list);
			}
			list.addTo(win);
		}
	}
	
	protected Route clone() {
		Route clone = new Route();
		clone.startBlock = startBlock;
		clone.startDirection = startDirection;
		clone.endBlock = endBlock;
		clone.endDirection = endDirection;
		clone.contacts = new Vector<Contact>(contacts);
		clone.signals = new Vector<Signal>(signals);
		clone.turnouts = new HashMap<>(turnouts);
		clone.path = new Vector<>(path);
		return clone;
	}
	
	public void complete() {
		if (contacts.size()>1) { // mindestens 2 Kontakte: erster Kontakt aktiviert Block, vorletzter Kontakt leitet Bremsung ein
			add(contacts.firstElement().trigger(),new ActivateRoute());
			Contact nextToLastContact = contacts.get(contacts.size()-2);			
			add(nextToLastContact.trigger(),new SetSpeed(30));			
			add(nextToLastContact.trigger(),new SetSignalsToStop());
		}
		if (!contacts.isEmpty()) {
			Contact lastContact = contacts.lastElement(); 
			add(lastContact.trigger(), new SetSpeed(0)); 
			add(lastContact.trigger(), new FinishRoute());
		}
	}

	/**
	 * Kontakt der Route aktivieren
	 * @param contact
	 * @param train
	 */
	public void contact(Contact contact) {
		LOG.debug("{} on {} activated {}.",train,this,contact);
		ActionList actions = triggers.get(contact.trigger());
		if (actions == null) return;
		Context context = new Context(contact);
		actions.fire(context);
	}
	
	public Vector<Contact> contacts() {
		return new Vector<>(contacts);
	}
		
	public Block endBlock() {
		return endBlock;
	}	
	
	public void finish() throws IOException {
		startBlock.train(null);		
		train.route = null;		
		unlock();
		endBlock.train(train.heading(endDirection.inverse()));
		train = null;
	}
	
	public boolean free() {
		for (int i=1; i<path.size(); i++) { 
			if (!path.get(i).free()) return false;
		}
		return true;
	}
	
	private String generateName() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<path.size();i++) {
			Tile tile = path.get(i);
			if (i>0) sb.append("-");
			if (tile instanceof Block) {
				sb.append(((Block)tile).name);
				if (i>0) break; // Kontakt nach dem Ziel-Block nicht mitnehmen
			} else {
				sb.append(tile.id());
			}
		}
		return sb.toString();
	}
	
	public int id() {
		if (id == 0) id = generateName().hashCode();
		return id;
	}
		
	/**
	 * creates a json representation of this route
	 * @return
	 */
	public String json() {
		JSONObject json = new JSONObject();
		
		json.put(ID, id());
		Vector<String> tileIds = new Vector<String>();
		for (Tile t : this.path) tileIds.add(t.id());
		json.put(PATH, tileIds);
		
		Vector<String> signalIds = new Vector<String>(); // list all signals affecting this route 
		for (Tile t : this.signals) signalIds.add(t.id());
		json.put(SIGNALS, signalIds);
		
		JSONArray turnouts = new JSONArray();
		for (Entry<Turnout, State> entry : this.turnouts.entrySet()) {
			Turnout t = entry.getKey();
			turnouts.put(new JSONObject(Map.of(Turnout.ID,t.id(),Turnout.STATE,entry.getValue())));
		}
		json.put(TURNOUTS, turnouts);
		json.put(START_DIRECTION, startDirection);
		json.put(END_DIRECTION, endDirection);
		
		JSONArray jTriggers = new JSONArray();
		for (Entry<String, ActionList> entry : triggers.entrySet()) {
			JSONObject trigger = new JSONObject();
			trigger.put(TRIGGER, entry.getKey());
			ActionList actionList = entry.getValue();
			trigger.put(ACTIONS, actionList.json());
			
			jTriggers.put(trigger);

		}
		if (!jTriggers.isEmpty()) json.put(ACTION_LISTS, jTriggers);
		
		String name = name();		
		if (name != null) json.put(NAME, name);

		return json.toString();
	}
	
	private Route load(JSONObject json,Plan plan) {
		if (json.has(ID)) id = json.getInt(ID);
		JSONArray pathIds = json.getJSONArray(PATH);
		startDirection = Direction.valueOf(json.getString(START_DIRECTION));
		endDirection = Direction.valueOf(json.getString(END_DIRECTION));
		for (Object tileId : pathIds) {
			Tile tile = plan.get((String) tileId,false);
			if (startBlock == null) {
				start((Block) tile, startDirection);
			} else if (tile instanceof Block) { // make sure, endDirection is set on last block
				add(tile,endDirection);
			} else {
				add(tile, null);
			}
		}
		if (json.has(NAME)) name(json.getString(NAME));
		if (json.has(TURNOUTS)) {
			JSONArray turnouts = json.getJSONArray(TURNOUTS);
			for (int i=0; i<turnouts.length();i++) {
				JSONObject jTurnout = turnouts.getJSONObject(i);
				Turnout turnout = (Turnout) plan.get(jTurnout.getString(Turnout.ID), false);
				addTurnout(turnout, Turnout.State.valueOf(jTurnout.getString(Turnout.STATE)));
			}
		}
		if (json.has(SIGNALS)) {
			for (Object signalId : json.getJSONArray(SIGNALS)) addSignal((Signal) plan.get((String) signalId, false));
		}
		if (json.has(ACTIONS)) loadActions(json.getJSONArray(ACTIONS));
		if (json.has(ACTION_LISTS)) loadActions(json.getJSONArray(ACTION_LISTS));
		return plan.registerRoute(this);
	}
	
	private void loadActions(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			String trigger = json.getString(TRIGGER);
			JSONArray actions = json.getJSONArray("actions");
			for (int k=0; k<actions.length(); k++) {
				try {
					Action action = Action.load(actions.getJSONObject(k));
					LOG.debug("Loaded {}",action);
					add(trigger, action);					
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException| InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | JSONException e) {
					LOG.warn("Was not able to load action: ",e);
				}
				
			}			
		}
	}

	public static void loadAll(String filename, Plan plan) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
			JSONObject json = new JSONObject(line);
			new Route().load(json,plan);
			
			line = file.readLine();
		}
		file.close();
	}
	
	public boolean lock() {		
		ArrayList<Tile> lockedTiles = new ArrayList<Tile>();
		try {
			for (Tile tile : path) lockedTiles.add(tile.lock(this));
		} catch (IOException e) {
			for (Tile tile: lockedTiles) try {
				tile.unlock();
			} catch (IOException inner) {
				LOG.warn("Was not able to unlock {}!",tile,inner);
			}
			return false;
		}
		return true;
	}
	
	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}
	
	public String name() {
		String name = names.get(id());
		if (name == null) {			
			name = generateName();
			name(name);
		}
		return name;
	}

	public void name(String name) {
		names.put(id(),name);
	}
	
	public Vector<Tile> path() {
		Vector<Tile> result = new Vector<Tile>();
		if (path != null) result.addAll(path);
		return result;
	}
	
	public Window properties() {	
		Window win = new Window("route-properties",t("Properties of {}",this));
		addFormTo(win);		
		addBasicPropertiesTo(win);
		addTurnoutsTo(win);
		addContactsTo(win);

		return win;
	}
	
	public static void saveAll(Collection<Route> routes, String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		file.write("[\n");
		int count = 0;
		for (Route route : routes) {			
			file.write(route.json());
			if (++count < routes.size()) file.write(",");
			file.write("\n");
		}
		file.write("]");
		file.close();
	}

	public void setLast(State state) {
		if (state == null || state == State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) addTurnout((Turnout) lastTile,state);
	}
	
	public boolean setSignals(String state) throws IOException {
		for (Signal signal : signals) {
			if (!signal.state(state == null ? Signal.GO : state)) return false;
		}
		return true;
	}
	
	public boolean setTurnouts() {
		Turnout turnout = null;
		for (Entry<Turnout, State> entry : turnouts.entrySet()) try {
			turnout = entry.getKey();
			State targetVal = entry.getValue();
			if (!turnout.state(targetVal).succeeded()) return false;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
		} catch (IOException e) {
			LOG.warn("Was not able to switch turnout {}!",turnout,e);
			return false;
		}
		return true;
	}
	
	public Route start(Block block,Direction from) {
		// add those fields to clone, too!
		contacts = new Vector<Contact>();
		signals = new Vector<Signal>();
		path = new Vector<Tile>();
		turnouts = new HashMap<>();
		startBlock = block;
		startDirection = from;
		path.add(block);
		return this;
	}

	public Block startBlock() {
		return startBlock;
	}		
	
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+name()+")";
	}
	
	public boolean train(Train train) {
		if (this.train != null && this.train != train) return false;
		this.train = train;
		return true;
	}
	
	public Route unlock() throws IOException {
		setSignals(Signal.STOP);
		for (Tile tile : path) tile.unlock();
		return this;
	}

	public void update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		if (params.containsKey(NAME)) name(params.get(NAME));
	}
}
