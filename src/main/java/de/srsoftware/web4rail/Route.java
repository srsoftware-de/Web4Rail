package de.srsoftware.web4rail;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.actions.FinishRoute;
import de.srsoftware.web4rail.actions.SetSignal;
import de.srsoftware.web4rail.actions.SetSpeed;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
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
public class Route extends BaseClass implements Comparable<Route>{
	private static final Logger LOG             = LoggerFactory.getLogger(Route.class);

	private static final String ACTIONS = "actions";
	private static final String ACTION_LISTS = "action_lists";
	private static final String CONDITIONS = "conditions";
	private static final String DROP_CONDITION = "drop_condition";
  	private static final String END_DIRECTION   = "direction_end";
	private static final String ROUTES = "routes";
	private static final String SETUP_ACTIONS = "setup_actions";
	private static final String START_ACTIONS = "start_actions";
	private static final String START_DIRECTION = "direction_start";
	private static final String TRIGGER = "trigger";
	static final String NAME     = "name";
	static final String PATH     = "path";
	static final String SIGNALS  = "signals";
	static final String TURNOUTS = "turnouts";

	private static HashMap<Integer, String> names = new HashMap<Integer, String>(); // maps id to name. needed to keep names during plan.analyze()

	private Vector<Condition>              conditions = new Vector<Condition>();
	private Vector<Contact>                contacts;
	private boolean                        disabled = false;
	private Block                          endBlock = null;
	public  Direction					   endDirection;
	private int                            id;
	private Vector<Tile>                   path;
	private Vector<Signal>                 signals;
	public  Train                          train;
	private HashMap<String,ActionList>     triggers = new HashMap<String, ActionList>();
	private HashMap<Turnout,Turnout.State> turnouts;
	private ActionList                     setupActions = new ActionList();
	private ActionList                     startActions = new ActionList();
	private Block                          startBlock = null;
	public  Direction 					   startDirection;
	private HashSet<Contact>			   triggeredContacts = new HashSet<>();               
	
	/**
	 * process commands from the client
	 * @param params
	 * @return
	 * @throws IOException 
	 */
	public static Object action(HashMap<String, String> params) throws IOException {
		Route route = plan.route(Integer.parseInt(params.get(ID)));
		if (isNull(route)) return t("Unknown route: {}",params.get(ID));
		switch (params.get(ACTION)) {
			case ACTION_DROP:
				String message = plan.remove(route);
				String tileId = params.get(Tile.class.getSimpleName());
				if (isSet(tileId)) {
					Tile tile = plan.get(tileId, false);
					if (isSet(tile)) {
						plan.stream(message);
						return tile.propMenu();
					}
				}
				return message;
			case ACTION_PROPS:
				return route.properties(params);
			case ACTION_UPDATE:
				return route.update(params,plan);
			case DROP_CONDITION:
				return route.dropCodition(params);
		}
		return t("Unknown action: {}",params.get(ACTION));
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
		if (isNull(actions)) {
			actions = new ActionList();
			triggers.put(trigger, actions);
		}
		actions.add(action);
	}
	
	private void addBasicPropertiesTo(Window win) {
		if (isSet(train)) link("span",Map.of(REALM,REALM_TRAIN,ID,train.id,ACTION,ACTION_PROPS),t("Train: {}",train)).addTo(win);
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
	
	private void addConditionsTo(Window win) {
		new Tag("h4").content(t("Conditions")).addTo(win);		
		if (!conditions.isEmpty()) {
			Tag list = new Tag("ul");
			for (Condition condition : conditions) {
				Tag li = new Tag("li");
				link("span",Map.of(REALM,REALM_CONDITION,ID,condition.id(),ACTION,ACTION_PROPS,CONTEXT,REALM_ROUTE+":"+id),condition).addTo(li);
				Map<String, Object> params = Map.of(REALM,REALM_ROUTE,ID,id(),ACTION,DROP_CONDITION,REALM_CONDITION,condition.id());
				new Button(t("delete"), params).addTo(li.content(NBSP)).addTo(list);
			}
			list.addTo(win);
		}

		new Tag("div").content(t("Route will only be available, if all conditions are fulfilled.")).addTo(win);
		Form form = new Form("action-prop-form-"+id);
		Fieldset fieldset = new Fieldset(t("Add condition"));
		new Input(REALM,REALM_ROUTE).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);

		Condition.selector().addTo(fieldset);
		new Button(t("Add condition"),form).addTo(fieldset).addTo(form).addTo(win);
	}
	
	private void addContactsTo(Window win) {
		if (!contacts.isEmpty()) {
			new Tag("h4").content(t("Actions and contacts")).addTo(win);
			Tag list = new Tag("ol");
			
			Tag setup = new Tag("li").content(t("Setup actions")+NBSP);
			setupActions.addTo(setup, context());
			setup.addTo(list);

			Tag start = new Tag("li").content(t("Start actions")+NBSP);
			startActions.addTo(start, context());
			start.addTo(list);

			for (Contact c : contacts) {
				Tag link = Plan.addLink(c,c+NBSP,list);
				ActionList actions = triggers.get(c.trigger());
				if (isNull(actions)) {
					actions = new ActionList();
					triggers.put(c.trigger(), actions);
				}
				actions.addTo(link,context());
			}
			list.addTo(win);
		}
	}
	
	private void addFormTo(Window win, HashMap<String, String> params) {
		Form form = new Form("route-"+id+"-props");
		new Input(ACTION, ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_ROUTE).hideIn(form);
		new Input(ID,id()).hideIn(form);
		if (params.containsKey(CONTEXT)) new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		new Input(NAME, name()).style("width: 80%").addTo(new Label(t("name:")+NBSP)).addTo(form);
		new Checkbox(DISABLED, t("disabled"), disabled).addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);
	}
	
	public void addPropertiesFrom(Route existingRoute) {
		LOG.debug("addPropertiesFrom({})",existingRoute);
		disabled = existingRoute.disabled;
		setupActions.addActionsFrom(existingRoute.setupActions);
		for (Entry<String, ActionList> entry : triggers.entrySet()) {
			String trigger = entry.getKey();
			ActionList existingActionList = existingRoute.triggers.get(trigger);
			if (isSet(existingActionList)) {
				LOG.debug("found action list for {} on existing route {}: {}",trigger,existingRoute,existingActionList);
				ActionList newActionList = entry.getValue();
				newActionList.addActionsFrom(existingActionList);
			}			
		}
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
				Plan.addLink(turnout, turnout+": "+t(entry.getValue().toString()), list);
			}
			list.addTo(win);
		}
	}
	
	/**
	 * checks, whether the route may be used in a given context
	 * @param context
	 * @return false, if any of the associated conditions is not fulfilled
	 */
	public boolean allowed(Context context) {
		if (disabled) return false;
		for (Condition condition : conditions) {
			if (!condition.fulfilledBy(context)) return false;
		}
		return true;
	}
	
	public Route begin(Block block,Direction from) {
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

	@Override
	public int compareTo(Route other) {
		return name().compareTo(other.name());
	}
	
	public Route complete() {
		if (contacts.size()>1) { // mindestens 2 Kontakte: erster Kontakt aktiviert Block, vorletzter Kontakt leitet Bremsung ein
			Contact nextToLastContact = contacts.get(contacts.size()-2);
			String trigger = nextToLastContact.trigger();
			add(trigger,new SetSpeed().to(30));
			for (Signal signal : signals) add(trigger,new SetSignal().set(signal).to(Signal.STOP));
		}
		if (!contacts.isEmpty()) {
			Contact lastContact = contacts.lastElement(); 
			add(lastContact.trigger(), new SetSpeed()); 
			add(lastContact.trigger(), new FinishRoute());
		}
		for (Signal signal : signals) setupActions.add(new SetSignal().set(signal).to(Signal.GO));
		startActions.add(new SetSpeed().to(100));
		return this;
	}

	/**
	 * Kontakt der Route aktivieren
	 * @param contact
	 * @param trainHead
	 */
	public void contact(Contact contact) {
		if (triggeredContacts.contains(contact)) return; // don't trigger contact a second time
		triggeredContacts.add(contact);
		LOG.debug("{} on {} activated {}.",train,this,contact);
		traceTrainFrom(contact);
		ActionList actions = triggers.get(contact.trigger());
		if (isNull(actions)) return;
		Context context = new Context(contact);
		actions.fire(context);
	}

	public Vector<Contact> contacts() {
		return new Vector<>(contacts);
	}
	
	public String context() {
		return REALM_ROUTE+":"+id();
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	
	private Object dropCodition(HashMap<String, String> params) {
		String condId = params.get(REALM_CONDITION);
		if (isSet(condId)) {
			int cid = Integer.parseInt(condId);
			for (Condition condition : conditions) {
				if (condition.id() == cid) {
					conditions.remove(condition);
					break;
				}
			}
		}
		return properties(params);
	}
		
	public Block endBlock() {
		return endBlock;
	}	
	
	public void finish() {
		setSignals(Signal.STOP);
		for (Tile tile : path) tile.setRoute(null);
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Contact) {
			lastTile.set(null);
			if (isSet(train)) train.removeFromTrace(lastTile);
		}
		if (isSet(train)) { 
			train.set(endBlock);
			train.heading(endDirection.inverse());
			if (endBlock == train.destination()) {
				train.destination(null).quitAutopilot();
				plan.stream(t("{} reached it`s destination!",train));
			} else {
				train.setWaitTime(endBlock.getWaitTime(train));
			}
			if (train.route == this) train.route = null;
		}
		train = null;
		triggeredContacts.clear();
	}
	
	public boolean fireSetupActions(Context context) {
		return setupActions.fire(context);
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
		
	public boolean isFreeFor(Train newTrain) {
		for (int i=1; i<path.size(); i++) { 
			if (!path.get(i).isFreeFor(newTrain)) return false;
		}
		return true;
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
		
		JSONArray jConditions = new JSONArray();
		for (Condition condition : conditions) jConditions.put(condition.json());
		if (!jConditions.isEmpty()) json.put(CONDITIONS, jConditions);
		
		JSONArray jTriggers = new JSONArray();
		for (Entry<String, ActionList> entry : triggers.entrySet()) {
			JSONObject trigger = new JSONObject();
			trigger.put(TRIGGER, entry.getKey());
			ActionList actionList = entry.getValue();
			trigger.put(ACTIONS, actionList.json());
			
			jTriggers.put(trigger);

		}
		if (!jTriggers.isEmpty()) json.put(ACTION_LISTS, jTriggers);
		if (!setupActions.isEmpty()) json.put(SETUP_ACTIONS, setupActions.json());
		if (!startActions.isEmpty()) json.put(START_ACTIONS, startActions.json());
		
		String name = name();		
		if (isSet(name)) json.put(NAME, name);
		
		if (disabled) json.put(DISABLED, true);

		return json.toString();
	}
	
	private Route load(JSONObject json,Plan plan) {
		if (json.has(ID)) id = json.getInt(ID);
		if (json.has(NAME)) name(json.getString(NAME));
		JSONArray pathIds = json.getJSONArray(PATH);
		startDirection = Direction.valueOf(json.getString(START_DIRECTION));
		endDirection = Direction.valueOf(json.getString(END_DIRECTION));
		for (Object tileId : pathIds) {
			Tile tile = plan.get((String) tileId,false);
			if (isNull(tile)) {
				continue;
			}
			if (isNull(startBlock)) {
				begin((Block) tile, startDirection);
			} else if (tile instanceof Block) { // make sure, endDirection is set on last block
				add(tile,endDirection);
			} else {
				add(tile, null);
			}
		}
		if (isNull(path) || path.isEmpty()) {
			LOG.warn("{} has no tiles. It will be ignored.",this);
			return null;
		}
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
		if (json.has(ACTION_LISTS)) loadActions(json.getJSONArray(ACTION_LISTS));
		if (json.has(CONDITIONS)) loadConditions(json.getJSONArray(CONDITIONS));
		if (json.has(SETUP_ACTIONS)) setupActions = ActionList.load(json.getJSONArray(SETUP_ACTIONS));
		if (json.has(START_ACTIONS)) startActions = ActionList.load(json.getJSONArray(START_ACTIONS));
		if (json.has(DISABLED)) disabled = json.getBoolean(DISABLED);
		return plan.registerRoute(this);
	}
	
	private void loadActions(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			String trigger = json.getString(TRIGGER);
			ActionList actionList = ActionList.load(json.getJSONArray(ACTIONS));
			triggers.put(trigger, actionList);
		}
	}

	public static void loadAll(String filename, Plan plan) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		JSONTokener tokener = new JSONTokener(fis);
		JSONObject json = new JSONObject(tokener);
		JSONArray routes = json.getJSONArray(ROUTES);
		for (Object o : routes) {
			if (o instanceof JSONObject) new Route().load((JSONObject)o, plan);
		}
		fis.close();
	}
	
	private void loadConditions(JSONArray arr) {
		for (int i=0; i<arr.length(); i++) {
			JSONObject json = arr.getJSONObject(i);
			Condition condition = Condition.create(json.getString(TYPE));
			if (isSet(condition)) conditions.add(condition.load(json));
		}
	}
	
	public boolean lock() {
		Vector<Tile> alreadyLocked = new Vector<Tile>();
		boolean success = true;
		for (Tile tile : path) {
			try {
				tile.setRoute(this);
			} catch (IllegalStateException e) {
				success = false;
				break;
			}			
		}
		if (!success) for (Tile tile :alreadyLocked) {
			tile.setRoute(null);
		}
		return success;
	}
	
	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}
	
	public String name() {
		String name = names.get(id());
		if (isNull(name)) {			
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
		if (isSet(path)) result.addAll(path);
		return result;
	}
	
	public Window properties(HashMap<String, String> params) {	
		Window win = new Window("route-properties",t("Properties of {}",this));
		addFormTo(win,params);		
		addBasicPropertiesTo(win);
		addTurnoutsTo(win);
		addConditionsTo(win);
		addContactsTo(win);

		return win;
	}
	
	public boolean reset() {
		setSignals(Signal.STOP);
		for (Tile tile : path) tile.setRoute(null);
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Contact) {
			lastTile.set(null);
			if (isSet(train)) train.removeFromTrace(lastTile);
		}
		if (isSet(train)) {
			train.set(startBlock);
			train.heading(startDirection);
			if (train.route == this) train.route = null;
			train = null;
		}	
		triggeredContacts.clear();
		return true;
	}

	public static void saveAll(Collection<Route> routes, String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		file.write("{\""+ROUTES+"\":[\n");
		int count = 0;
		for (Route route : routes) {			
			file.write(route.json());
			if (++count < routes.size()) file.write(",");
			file.write("\n");
		}
		file.write("]}");
		file.close();
	}

	public void setLast(State state) {
		if (isNull(state) || state == State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) addTurnout((Turnout) lastTile,state);
	}
	
	public boolean setSignals(String state) {
		for (Signal signal : signals) {
			if (!signal.state(isNull(state) ? Signal.GO : state)) return false;
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
	
	public String shortName() {
		String[] parts = name().split("-");
		return parts[0].trim()+"–"+parts[parts.length-1].trim();
	}

	public Block startBlock() {
		return startBlock;
	}		
	
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+(isSet(train)?train+":":"")+name()+")";
	}
	
	private void traceTrainFrom(Tile tile) {
		Vector<Tile> trace = new Vector<Tile>();
		for (Tile t:path) {
			trace.add(t);
			if (t == tile) break;
		}
		if (isSet(train)) train.addToTrace(trace);
	}
	
	public boolean train(Train newTrain) {
		if (isSet(train) && newTrain != train) return false;
		train = newTrain;
		return isSet(train) ? startActions.fire(new Context(this)) : true;
	}
	
	public Route unlock() throws IOException {
		// TODO
		return this;
	}

	public Object update(HashMap<String, String> params,Plan plan) {
		LOG.debug("update({})",params);
		String name = params.get(NAME);
		if (isSet(name)) name(name);
		
		disabled = "on".equals(params.get(DISABLED));
		
		Condition condition = Condition.create(params.get(REALM_CONDITION));
		if (isSet(condition)) {
			conditions.add(condition);
			return properties(params);
		}
		String message = t("{} updated.",this); 
		if (params.containsKey(CONTEXT)) {
			plan.stream(message);
			return plan.showContext(params);
		}
		return message;
	}
}
