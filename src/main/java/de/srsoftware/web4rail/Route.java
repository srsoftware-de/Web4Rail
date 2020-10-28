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
import de.srsoftware.web4rail.actions.ActivateRoute;
import de.srsoftware.web4rail.actions.FinishRoute;
import de.srsoftware.web4rail.actions.SetSignalsToStop;
import de.srsoftware.web4rail.actions.SpeedReduction;
import de.srsoftware.web4rail.actions.TurnTrain;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

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
	private HashMap<String,Vector<Action>> triggers = new HashMap<String, Vector<Action>>();
	private int id;
	private static HashMap<Integer, String> names = new HashMap<Integer, String>(); // maps id to name. needed to keep names during plan.analyze()
	public Train train;
	private Block startBlock = null,endBlock;
	private static final String START_DIRECTION = "direction_start";
	private static final String END_DIRECTION = "direction_end";
	
	public Direction startDirection;
	private Direction endDirection;
	private Plan plan;

	private static final String TRIGGER = "trigger";
	private static final String ACTIONS = "actions";
	private static final String ACTION_ID = "action_id";
	
	private Tag actionTypeForm(Contact contact) {
		String formId ="add-action-to-contact-"+contact.id();
		Tag typeForm = new Form(formId);
		new Input(REALM, REALM_ROUTE).hideIn(typeForm);
		new Input(ID,id()).hideIn(typeForm);
		new Input(ACTION,ACTION_ADD_ACTION).hideIn(typeForm);
		new Input(CONTACT,contact.id()).hideIn(typeForm);
		Select select = new Select(TYPE);
		List<Class<? extends Action>> classes = List.of(
				SpeedReduction.class,
				SetSignalsToStop.class,
				FinishRoute.class,
				TurnTrain.class);
		for (Class<? extends Action> clazz : classes) select.addOption(clazz.getSimpleName());
		select.addTo(new Label("Action type:")).addTo(typeForm);
		return new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(typeForm);
	}
	
	/**
	 * Route wurde von Zug betreten
	 * @throws IOException 
	 */
	public void activate() throws IOException {
		LOG.debug("{} aktiviert.",this);
		for (Tile tile : path) tile.train(train);
	}
	
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
	
	public void addAction(String trigger, Action action) {
		Vector<Action> actions = triggers.get(trigger);
		if (actions == null) {
			actions = new Vector<Action>();
			triggers.put(trigger, actions);
		}
		actions.add(action);
	}
	
	public Object addActionForm(HashMap<String, String> params) {
		String contactId = params.get(CONTACT);
		Tile tag = plan.get(contactId, false);
		if (!(tag instanceof Contact)) return t("No contact id passed to request!");
		Contact contact = (Contact) tag;
		Window win = new Window("add-action-form", t("Add action to contact on route"));		
		new Tag("div").content("Route: "+this).addTo(win);
		new Tag("div").content("Contact: "+contact).addTo(win);
		
		String type = params.get(TYPE);
		if (type == null) return (actionTypeForm(contact).addTo(win));
		switch (type) {
			case "FinishRoute":
				addAction(contact.trigger(),new FinishRoute(id()));
				break;
			case "SpeedReduction":
				return SpeedReduction.propForm(params,this,contact);
			case "SetSignalsToStop":
				addAction(contact.trigger(),new SetSignalsToStop(id()));
				break;
			case "TurnTrain":
				addAction(contact.trigger(),new TurnTrain(id()));
				break;
			default:
				return win;			
		}
		plan.stream("Action added!");
		return properties();
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
			Tag list = new Tag("ul");
			for (Contact c : contacts) {
				Tag link = Plan.addLink(c,c.toString(),list);
				JSONObject json = new JSONObject(Map.of(
						REALM,REALM_ROUTE,
						ID,id,
						ACTION,ACTION_ADD_ACTION,
						CONTACT,c.id()));
				new Button(t("add action"),json).addTo(link);
				Vector<Action> actions = triggers.get(c.trigger());
				if (actions != null && !actions.isEmpty()) {
					Tag ul = new Tag("ul");
					boolean first = true;
					for (Action action : actions) {
						json.put(ACTION_ID, action.toString());

						Tag act = new Tag("li").content(action.toString());
						if (!first) {
							json.put(ACTION, ACTION_MOVE);
							new Button("↑",json).addTo(act);
						}
						json.put(ACTION, ACTION_DROP);
						new Button("-",json).addTo(act);
						act.addTo(ul);
						first = false;
					}
					ul.addTo(link);
				}
			}
			list.addTo(win);
		}
	}
	
	private void addFormTo(Window win) {
		Form form = new Form();
		new Input(ACTION, ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_ROUTE).hideIn(form);
		new Input(ID,id()).hideIn(form);
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name()).addTo(label);		
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
			addAction(contacts.firstElement().trigger(),new ActivateRoute(id()));
			Contact nextToLastContact = contacts.get(contacts.size()-2);			
			addAction(nextToLastContact.trigger(),new SpeedReduction(id(),30));			
			addAction(nextToLastContact.trigger(),new SetSignalsToStop(id()));
		}
		if (!contacts.isEmpty()) {
			Contact lastContact = contacts.lastElement(); 
			addAction(lastContact.trigger(), new SpeedReduction(id(), 0)); 
			addAction(lastContact.trigger(), new FinishRoute(id()));
		}
	}

	/**
	 * Kontakt der Route aktivieren
	 * @param contact
	 * @param train
	 */
	public void contact(Contact contact) {
		LOG.debug("{} on {} activated {}.",train,this,contact);
		Vector<Action> actions = triggers.get(contact.trigger());
		if (actions == null) return;
		for (Action action : actions) {
			try {
				action.fire(contact.plan());
			} catch (IOException e) {
				LOG.warn("Action did not fire properly: {}",action,e);
			}
		}
	}
	
	public Vector<Contact> contacts() {
		return new Vector<>(contacts);
	}
	
	public Object dropAction(HashMap<String, String> params) {
		String actionId = params.get(ACTION_ID);
		if (actionId == null) return t("No action id passed to request!");
		String contactId = params.get(CONTACT);
		Tile tag = plan.get(contactId, false);
		if (!(tag instanceof Contact)) return t("No contact id passed to request!");
		Contact contact = (Contact) tag;
		Vector<Action> actions = triggers.get(contact.trigger());
		
		for (int i=0; i<actions.size(); i++) {
			if (actions.elementAt(i).toString().equals(actionId)) {
				actions.remove(i);
				plan.stream(t("removed {}.",actionId));
				return properties();
			}
		}
		plan.stream(t("No action \"{}\" assigned with {}!",actionId,contact));
		return properties();
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
		for (Entry<String, Vector<Action>> entry : triggers.entrySet()) {
			JSONObject trigger = new JSONObject();
			trigger.put(TRIGGER, entry.getKey());
			
			JSONArray jActions = new JSONArray();
			for (Action action : entry.getValue()) {
				jActions.put(action.json());
			}
			trigger.put(ACTIONS, jActions);
			
			jTriggers.put(trigger);

		}
		if (!jTriggers.isEmpty()) json.put(ACTIONS, jTriggers);
		
		String name = name();		
		if (name != null) json.put(NAME, name);

		return json.toString();
	}
	
	private Route load(JSONObject json,Plan plan) {
		this.plan = plan;
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
		return plan.registerRoute(this);
	}
	
	private void loadActions(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			String trigger = json.getString(TRIGGER);
			JSONArray actions = json.getJSONArray(ACTIONS);
			for (int k=0; k<actions.length(); k++) {
				try {
					Action action = Action.load(actions.getJSONObject(k));
					LOG.debug("Loaded {}",action);
					addAction(trigger, action);					
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
	
	public Object moveAction(HashMap<String, String> params) {
		String action_id = params.get(ACTION_ID);
		if (action_id == null) return t("No action id passed to request!");
		String contactId = params.get(CONTACT);
		Tile tag = plan.get(contactId, false);
		if (!(tag instanceof Contact)) return t("No contact id passed to request!");
		Contact contact = (Contact) tag;
		Vector<Action> actions = triggers.get(contact.trigger());
		
		for (int i=1; i<actions.size(); i++) {
			if (actions.elementAt(i).toString().equals(action_id)) {
				Action action = actions.remove(i);
				actions.insertElementAt(action, i-1);
				return properties();
			}
		}
		plan.stream(t("No action \"{}\" assigned with {}!",action_id,contact));
		return properties();
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
		for (Route route : routes) file.write(route.json()+"\n");
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
		return (Block) path.get(0);
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
