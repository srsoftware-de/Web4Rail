package de.srsoftware.web4rail;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.actions.BrakeStart;
import de.srsoftware.web4rail.actions.DelayedAction;
import de.srsoftware.web4rail.actions.FinishRoute;
import de.srsoftware.web4rail.actions.PreserveRoute;
import de.srsoftware.web4rail.actions.SetSignal;
import de.srsoftware.web4rail.actions.SetSpeed;
import de.srsoftware.web4rail.actions.SetTurnout;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.conditions.ConditionList;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.BrakeProcessor;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.BlockContact;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout;
/**
 * A route is a vector of tiles that leads from one block to another.
 * 
 * @author Stephan Richter, SRSoftware 2020-2021  
 *
 */
public class Route extends BaseClass {
	
	public enum State {
		FREE, LOCKED, PREPARED, STARTED;
	}
	public static final Logger LOG = LoggerFactory.getLogger(Route.class);

	private static final String ACTIONS = "actions";
	private static final String BRAKE_TIMES = "brake_times";
	private static final String CONDITION_LIST = "condition_list";
  	private static final String END_DIRECTION   = "direction_end";
	private static final String ROUTES = "routes";
	private static final String SETUP_ACTIONS = "setup_actions";
	private static final String START_ACTIONS = "start_actions";
	private static final String START_DIRECTION = "direction_start";
	static final String NAME     = "name";
	static final String PATH     = "path";
	static final String SIGNALS  = "signals";
	static final String TURNOUTS = "turnouts";
	private State state = State.FREE;
	public static boolean freeBehindTrain = true;

	private static final String ROUTE_START = "route_start";

	private static final String ROUTE_SETUP = "route_setup";

	private static HashMap<Id, String> names = new HashMap<Id, String>(); // maps id to name. needed to keep names during plan.analyze()

	private BrakeProcessor				   brakeProcessor = null;
	private HashMap<String,Integer>        brakeTimes = new HashMap<String, Integer>();
	private ConditionList                  conditions;
	private Vector<Contact>                contacts;
	private Context                        context; // this context is passed to actions
	private boolean                        disabled = false;
	private Block                          endBlock = null;
	public  Direction					   endDirection;
	private Vector<Tile>                   path;
	private Vector<Signal>                 signals;
	private Train                          train;
	private HashMap<String,ActionList>     triggeredActions = new HashMap<String, ActionList>();
	private HashMap<Turnout,Turnout.State> turnouts;
	private Block                          startBlock = null;
	public  Direction 					   startDirection;
	private HashSet<Contact>			   triggeredContacts = new HashSet<>();
	
	public Route() {
		conditions = new ConditionList();
		conditions.parent(this);
	}
		
	/**
	 * process commands from the client
	 * @param params
	 * @return
	 * @throws IOException 
	 */
	public static Object action(HashMap<String, String> params) throws IOException {
		Route route = BaseClass.get(Id.from(params));
		String action = params.get(ACTION);
		if (isNull(route) && !ACTION_AUTO.equals(action)) return t("Unknown route: {}",params.get(ID));
		switch (params.get(ACTION)) {
			case ACTION_AUTO:
				if (isSet(route)) return route.simplyfyName().properties();
				for (Route rt : BaseClass.listElements(Route.class)) rt.simplyfyName();
				return plan.properties(new HashMap<String, String>());
			case ACTION_DROP:
				route.remove();
				plan.stream(t("Removed {}.",route));				
				return plan.properties(new HashMap<String,String>());
			case ACTION_PROPS:
				return route.properties();
			case ACTION_START:
				route.set(new Context(route));
				route.prepare();
				route.context.clear();
				
				return route.properties();
			case ACTION_UPDATE:
				return route.update(params,plan);
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
			contacts.addAll(endBlock.contacts());
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
		ActionList actions = triggeredActions.get(trigger);
		if (isNull(actions)) {
			actions = new ActionList(this);
			triggeredActions.put(trigger, actions);
		}
		actions.add(action);
	}
	
	public void add(Condition condition) {
		conditions.add(condition);
	}
	
	public void addPropertiesFrom(Route existingRoute) {
		LOG.debug("addPropertiesFrom({})",existingRoute);
		disabled = existingRoute.disabled;

		for (Condition condition : existingRoute.conditions) { // bestehende Bedingungen der neuen zuweisen
			condition.parent(this);
			conditions.add(condition);			
		}
		conditions.forEach(condition -> existingRoute.conditions.removeChild(condition));
		for (Entry<String, ActionList> entry : triggeredActions.entrySet()) {
			String trigger = entry.getKey();
			ActionList existingActionList = existingRoute.triggeredActions.get(trigger);
			if (isSet(existingActionList)) {
				LOG.debug("found action list for {} on existing route {}: {}",trigger,existingRoute,existingActionList);
				existingActionList.forEach(action -> LOG.debug("OLD Action: {}",action));
				entry.getValue().merge(existingActionList);
			}			
		}
		brakeTimes = new HashMap<String, Integer>(existingRoute.brakeTimes);
	}
	
	void addSignal(Signal signal) {
		signals.add(signal);
	}
	
	void addTurnout(Turnout t, Turnout.State s) {
		turnouts.put(t, s);
	}
	
	/**
	 * checks, whether the route may be used in a given context
	 * @param context
	 * @return false, if any of the associated conditions is not fulfilled
	 */
	public boolean allowed(Context context) {
		if (disabled) return false;
		return conditions.fulfilledBy(context);
	}
	
	private Fieldset basicProperties() {
		Fieldset fieldset = new Fieldset(t("Route properties"));
		
		if (isSet(train)) train.link("span",t("Train")+": "+train).addTo(fieldset);
		Tag list = new Tag("ul");
		startBlock.link("li",t("Origin: {} to {}",startBlock.name,startDirection)).addTo(list);
		endBlock.link("li",t("Destination: {} from {}",endBlock.name,endDirection.inverse())).addTo(list);
		list.addTo(fieldset);
		
		if (!signals.isEmpty()) {
			new Tag("h4").content(t("Signals")).addTo(fieldset);
			list = new Tag("ul");
			for (Signal s : signals) Plan.addLink(s,s.toString(),list);
			list.addTo(fieldset);
		}
		
		this.button(t("Test"),Map.of(ACTION,ACTION_START)).addTo(fieldset);
		return fieldset;
	}
	
	public Integer brakeTime(String brakeId) {
		return brakeTimes.get(brakeId);
	}

	public void brakeTime(String brakeId, Integer newTimeStep) {
		brakeTimes.put(brakeId,newTimeStep);
	}
	
	private Fieldset brakeTimes() {
		Fieldset fieldset = new Fieldset(t("Brake time table"));
		Table table = new Table();
		table.addHead(t("Train"),t("Brake time¹, forward"),t("Brake time¹, reverse"));
		for (Train t : BaseClass.listElements(Train.class)) {
			Integer fTime = brakeTimes.get(t.brakeId());			 
			Integer rTime = brakeTimes.get(t.brakeId(true));
			table.addRow(t,isSet(fTime)? fTime+NBSP+"ms" : "–",isSet(rTime)? fTime+NBSP+"ms" : "–");			
		}
		table.clazz("brake-times").addTo(fieldset);
		new Tag("p").content(t("1) Duration between 5 {} steps during brake process.",speedUnit)).addTo(fieldset);
		return fieldset;
	}
				
	public Route begin(Block block,Direction to) {
		// add those fields to clone, too!
		startBlock = block;
		contacts = new Vector<Contact>(startBlock.contacts());
		signals = new Vector<Signal>();
		path = new Vector<Tile>();
		turnouts = new HashMap<>();
		startDirection = to;
		path.add(block);
		return this;
	}
	
	public void brakeStart() {
		if (isNull(train)) return;		
		brakeProcessor = new BrakeProcessor(this,train);
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
		clone.brakeTimes = new HashMap<String, Integer>(brakeTimes);
		return clone;
	}
	
	public Route complete() {
		if (contacts.size()>1) { // mindestens 2 Kontakte: erster Kontakt aktiviert Block, vorletzter Kontakt leitet Bremsung ein
			Contact nextToLastContact = contacts.get(contacts.size()-2);
			String trigger = nextToLastContact.trigger();
			add(trigger,new BrakeStart(this));
			add(trigger,new PreserveRoute(this));
			
			int count = 0;
			for (int i=0;i<contacts.size();i++) { // chose second contact, that is not a BlockContact
				Contact contact = contacts.get(i);
				if (contact instanceof BlockContact) continue;
				if (count++<1) continue;
				
				trigger = contact.trigger(); // second contact, that is not a BlockContact
				for (Signal signal : signals) add(trigger,new SetSignal(this).set(signal).to(Signal.RED));
				break;
			}
		}
		if (!contacts.isEmpty()) add(contacts.lastElement().trigger(), new FinishRoute(this));
		for (Entry<Turnout, Turnout.State> entry : turnouts.entrySet()) {
			Turnout turnout = entry.getKey();
			Turnout.State state = entry.getValue();
			add(ROUTE_SETUP,new SetTurnout(this).setTurnout(turnout).setState(state));
		}
		for (Signal signal : signals) add(ROUTE_SETUP,new SetSignal(this).set(signal).to(Signal.GREEN));
		if (signals.isEmpty()) {
			add(ROUTE_START,new SetSpeed(this).to(999));
		} else {
			DelayedAction da = new DelayedAction(this).setMinDelay(1000).setMaxDelay(7500);
			add(ROUTE_START,da.add(new SetSpeed(this).to(999)));
		}
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
		ActionList actions = triggeredActions.get(contact.trigger());
		LOG.debug("Contact has id {} / trigger {} and is assigned with {}",contact.id(),contact.trigger(),isNull(actions)?t("nothing"):actions);
		if (isNull(actions)) return;
		actions.fire(context,"Route.Contact("+contact.addr()+")");
	}

	public Vector<Contact> contacts() {
		return new Vector<>(contacts);
	}
	
	private Fieldset contactsAndActions() {
		Fieldset win = new Fieldset(t("Actions and contacts"));
		Tag list = new Tag("ol");
		
		Tag setup = new Tag("li").content(t("Setup actions")+COL);
		ActionList setupActions = triggeredActions.get(ROUTE_SETUP);
		if (isNull(setupActions)) {
			setupActions = new ActionList(this);
			triggeredActions.put(ROUTE_SETUP, setupActions);
		}
		setupActions.list().addTo(setup).addTo(list);

		Tag start = new Tag("li").content(t("Start actions")+COL);
		ActionList startActions = triggeredActions.get(ROUTE_START);
		if (isNull(startActions)) {
			startActions = new ActionList(this);
			triggeredActions.put(ROUTE_START, startActions);
		}
		startActions.list().addTo(start).addTo(list);

		for (Contact c : contacts) {
			Tag item = c.link("span", c).addTo(new Tag("li")).content(NBSP);
			ActionList actions = triggeredActions.get(c.trigger());
			if (isNull(actions)) {
				actions = new ActionList(this);
				triggeredActions.put(c.trigger(), actions);
			}
			actions.list().addTo(item).addTo(list);
		}
		list.addTo(win);
		return win;
	}
	
	public Context context() {
		return context.clone();
	}
	
	public void dropBraketimes(String...brakeIds) {
		for (String brakeId : brakeIds) brakeTimes.remove(brakeId);
	}
	
	public Block endBlock() {
		return endBlock;
	}	
	
	public void finish() {
		LOG.debug("{}.finish()",this);
		
		if (isSet(train)) {
			Route nextRoute = train.nextRoute();
			if (isSet(nextRoute)) {
				LOG.debug("{} has next route: {}",train,nextRoute);
				if (isSet(brakeProcessor)) brakeProcessor.abort();
			} else {
				LOG.debug("{} has no next route.",train);
				if (isSet(brakeProcessor)) {					
					brakeProcessor.finish();
				} else train.setSpeed(0);
			}
		}
		brakeProcessor = null;
		
		free();
		
		if (isSet(train)) {
			moveTrainToEndBlock();
			if (train.route() == this) train.route(null);
			train = null;
		}		
	
		state = State.FREE;
	}
	
	/**
	 * sets all signals of this route to RED,
	 * frees all tiles occupied by this route
	 */
	private void free() {
		LOG.debug("{}.free()",this);
		context.clear(); // prevent delayed actions from firing after route has finished

		setSignals(Signal.RED);
		for (Tile tile : path) try { // remove route from tiles on path
			tile.unset(this);
		} catch (IllegalArgumentException e) {}
		
/*		Tile lastTile = path.lastElement();
		if (lastTile instanceof Contact) {
			lastTile.setTrain(null);
			if (isSet(train)) train.removeChild(lastTile);
		}*/
	}

	private String generateName() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<path.size();i++) {
			Tile tile = path.get(i);
			if (i>0) sb.append("-");
			if (tile instanceof Block) {
				sb.append(" ").append(((Block)tile).name).append(" ");
				if (i>0) break; // Kontakt nach dem Ziel-Block nicht mitnehmen
			} else {
				sb.append("("+tile.x+","+tile.y+")");
			}
		}
		return sb.toString().trim();
	}
	
	public Id id() {
		if (isNull(id)) id = new Id(""+(generateName().hashCode()));
		return id;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
		
	public boolean isFreeFor(Context context) {
		PathFinder.LOG.debug("{}.isFreeFor({})",this,context);
		for (int i=1; i<path.size(); i++) {
			if (!path.get(i).isFreeFor(context)) {
				PathFinder.LOG.debug("{}.isFreeFor(...) → false",this);
				return false;
			}
		}
		return true;
	}
	
	/**
	 * creates a json representation of this route
	 * @return
	 */
	public JSONObject json() {
		JSONObject json = super.json();
		Vector<String> tileIds = new Vector<String>();
		for (Tile t : this.path) tileIds.add(t.id().toString());
		json.put(PATH, tileIds);
		
		Vector<String> signalIds = new Vector<String>(); // list all signals affecting this route 
		for (Tile t : this.signals) signalIds.add(t.id().toString());
		json.put(SIGNALS, signalIds);
		
		JSONArray turnouts = new JSONArray();
		for (Entry<Turnout, Turnout.State> entry : this.turnouts.entrySet()) {
			Turnout t = entry.getKey();
			turnouts.put(new JSONObject(Map.of(Turnout.ID,t.id().toString(),Turnout.STATE,entry.getValue())));
		}
		json.put(TURNOUTS, turnouts);
		json.put(START_DIRECTION, startDirection);
		json.put(END_DIRECTION, endDirection);
		
		json.put(BRAKE_TIMES, brakeTimes);
		
		if (!conditions.isEmpty()) {
			json.put(CONDITION_LIST, conditions.json());
		}
		
		JSONObject jActions = new JSONObject();
		for (Entry<String, ActionList> entry : triggeredActions.entrySet()) {
			String trigger = entry.getKey();
			ActionList lst = entry.getValue();
			jActions.put(trigger,lst.json());
		}
		json.put(ACTIONS, jActions);
		
		String name = name();		
		if (isSet(name)) json.put(NAME, name);
		
		if (disabled) json.put(DISABLED, true);

		return json;
	}
	
	private Route load(JSONObject json,Plan plan) {
		if (json.has(ID)) id = Id.from(json);
		if (json.has(NAME)) name(json.getString(NAME));
		JSONArray pathIds = json.getJSONArray(PATH);
		startDirection = Direction.valueOf(json.getString(START_DIRECTION));
		endDirection = Direction.valueOf(json.getString(END_DIRECTION));
		for (Object tileId : pathIds) {
			Tile tile = plan.get(new Id((String) tileId),false);
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
				Turnout turnout = (Turnout) plan.get(new Id(jTurnout.getString(Turnout.ID)), false);
				addTurnout(turnout, Turnout.State.valueOf(jTurnout.getString(Turnout.STATE)));
			}
		}
		if (json.has(SIGNALS)) {
			for (Object signalId : json.getJSONArray(SIGNALS)) addSignal((Signal) plan.get(new Id((String) signalId), false));
		}
		if (json.has(ACTIONS)) {
			loadActions(json.getJSONObject(ACTIONS));
		}
		if (json.has("action_lists")) { // TODO: this is legacy!
			JSONArray jarr = json.getJSONArray("action_lists");
			for (Object o : jarr) {
				if (o instanceof JSONObject) {
					JSONObject jo = (JSONObject) o;
					ActionList aList = new ActionList(this);
					String trigger = jo.getString("trigger");
					JSONArray jActions = jo.getJSONArray(ACTIONS);
					for (Object ja : jActions) {
						JSONObject jao = (JSONObject) ja;
						String type = jao.getString(TYPE);
						Action action = Action.create(type, aList);
						if (isSet(action)) {
							action.load(jao);
							aList.add(action);
						}
					}
					triggeredActions.put(trigger, aList);
				}
			}
		}
		if (json.has("conditions")) { // TODO: this is legacy!
			JSONArray jConditions = json.getJSONArray("conditions");
			for (Object o : jConditions) {
				if (o instanceof JSONObject) {
					JSONObject jo = (JSONObject) o;
					String type = jo.getString(TYPE);
					Condition condition = Condition.create(type);
					if (isSet(condition)) {
						condition.load(jo);
						conditions.add(condition);
					}					
				}
			}
		}
		if (json.has(CONDITION_LIST)) conditions.load(json.getJSONObject(CONDITION_LIST)).parent(this);
		if (json.has(SETUP_ACTIONS)) { // TODO: this is legacy!
			Object so = json.get(SETUP_ACTIONS);
			if (so instanceof JSONObject) {
				JSONObject jo = (JSONObject) so;
				ActionList setupActions = new ActionList(this);
				setupActions.load(jo).parent(this);
				triggeredActions.put(ROUTE_SETUP, setupActions);
			}
			if (so instanceof JSONArray) {
				JSONArray ja = (JSONArray) so;
				ActionList setupActions = new ActionList(this);
				for (Object o : ja) {
					if (o instanceof JSONObject) {
						JSONObject jo = (JSONObject) o;
						String type = jo.getString(TYPE);
						Action action = Action.create(type, setupActions);
						if (isSet(action)) {
							action.load(jo);
							setupActions.add(action);
						}
					}
				}
				triggeredActions.put(ROUTE_SETUP, setupActions);
			}			
		}
		if (json.has(START_ACTIONS)) { // TODO: this is legacy!
			Object so = json.get(START_ACTIONS);
			if (so instanceof JSONObject) {
				JSONObject jo = (JSONObject) so;
				ActionList startActions = new ActionList(this);
				startActions.load(jo).parent(this);
				triggeredActions.put(ROUTE_START, startActions);
			}
			if (so instanceof JSONArray) {
				JSONArray ja = (JSONArray) so;
				ActionList startActions = new ActionList(this);
				for (Object o : ja) {
					if (o instanceof JSONObject) {
						JSONObject jo = (JSONObject) o;
						String type = jo.getString(TYPE);
						Action action = Action.create(type, startActions);
						if (isSet(action)) {
							action.load(jo);
							startActions.add(action);
						}
					}
				}
				triggeredActions.put(ROUTE_START, startActions);
			}			
		
		}
		if (json.has(DISABLED)) disabled = json.getBoolean(DISABLED);
		if (json.has(BRAKE_TIMES)) {
			JSONObject dummy = json.getJSONObject(BRAKE_TIMES);
			dummy.keySet().forEach(key -> brakeTimes.put(key, dummy.getInt(key)));
		}
		return plan.registerRoute(this);
	}

	private void loadActions(JSONObject jsonObject) {
		for (String trigger : jsonObject.keySet()) {
			JSONObject json = jsonObject.getJSONObject(trigger);
			String type = json.getString(TYPE);
			ActionList actionList = Action.create(type, this);
			if (isNull(actionList)) continue;
			actionList.load(json);
			triggeredActions.put(trigger, actionList);
		}		
	}

	public static void loadAll(String filename, Plan plan) throws IOException {
		FileInputStream fis = new FileInputStream(filename);
		JSONTokener tokener = new JSONTokener(fis);
		JSONObject json = new JSONObject(tokener);
		JSONArray routes = json.getJSONArray(ROUTES);
		for (Object o : routes) {
			if (o instanceof JSONObject) {
				new Route().load((JSONObject)o, plan);
			}
		}
		fis.close();
	}
		
	public boolean lock() {
		return lockIgnoring(null);
	}
	
	public boolean lockIgnoring(Route ignoredRoute) {
		if (state == State.LOCKED || state == State.PREPARED || state == State.STARTED) return true;
		LOG.debug("{}.lockIgnoring({})",this,ignoredRoute);
		HashSet<Tile> ignoredPath = new HashSet<Tile>();
		if (isSet(ignoredRoute)) ignoredPath.addAll(ignoredRoute.path);
		boolean success = true;
		for (Tile tile : path) {
			if (ignoredPath.contains(tile)) continue;
			try {
				tile.setRoute(this);
			} catch (IllegalStateException e) {
				LOG.debug("{}.lockIgnoring(...) failed at {}, rolling back",this,tile);
				success = false;
				break;
			}			
		}
		if (success) state = State.LOCKED;
		return success;
	}
	
	private void moveTrainToEndBlock() {
		if (isNull(train)) return;
		LOG.debug("{}.moveTrainToEndBlock()",this);
		
		train.set(endBlock);
		traceTrainFrom(endBlock);
		train.heading(endDirection);
	
		if (endBlock == train.destination()) {
			train.destination(null); // unset old destination
			String destTag = train.destinationTag();
			if (isSet(destTag)) {
				LOG.debug("destination list: {}",destTag);
				String[] parts = destTag.split(Train.DESTINATION_PREFIX);
				for (int i=0; i<parts.length;i++) LOG.debug("  part {}: {}",i+1,parts[i]);
				String destId = parts[1];
				LOG.debug("destination tag: {}",destId);
				boolean turn = false;
				
				for (int i=destId.length()-1; i>0; i--) {
					switch (destId.charAt(i)) {
						case Train.FLAG_SEPARATOR:
							destId = destId.substring(0,i);
							i=0;
							break;
						case Train.TURN_FLAG:
							turn = true; 
							LOG.debug("Turn flag is set!");
							break;
					}
				}
				if (destId.equals(endBlock.id().toString())) { 
					if (turn) train.turn();
					
					// update destination tag: remove and add altered tag:
					train.removeTag(destTag);
					destTag = destTag.substring(parts[1].length()+1);
					if (destTag.isEmpty()) { // no further destinations
						destTag = null;
					} else train.addTag(destTag);
				}					
			}
			
			if (isNull(destTag)) {
				train.quitAutopilot();
				plan.stream(t("{} reached it`s destination!",train));
			}
		} else train.setWaitTime(endBlock.getWaitTime(train,train.direction()));
		if (startBlock.train() == train && !train.onTrace(startBlock)) startBlock.setTrain(null); // withdraw train from start block only if trace does not go back there
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
		if (name.isEmpty()) {
			names.remove(id());
		} else names.put(id(),name);
	}
	
	public Vector<Tile> path() {
		Vector<Tile> result = new Vector<Tile>();
		if (isSet(path)) result.addAll(path);
		return result;
	}
	
	public boolean prepare() {
		if (state == State.PREPARED || state == State.STARTED) return true;
		LOG.debug("{}.prepare()",this);
		ActionList setupActions = triggeredActions.get(ROUTE_SETUP);
		if (isSet(setupActions) && !setupActions.fire(context,this+".prepare()")) return false;
		state = State.PREPARED;
		return true;
	}
	
	private Tag previewScript() {
		Tag script = new Tag("script").attr("type", "text/javascript");
		for (Tile tile : path) {
			script.content("$('#"+tile.id()+"').addClass('preview');\n");
		}
		return script;
	}
	
	public Route prolong(Route nextRoute) {
		if (isSet(brakeProcessor)) brakeProcessor.setEndSpeed(nextRoute.startSpeed());
		return nextRoute;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {

		preForm.add(conditions.list(t("Route will only be available, if all conditions are fulfilled.")));
		preForm.add(contactsAndActions());

		Tag nameSpan = new Tag("span");
		new Input(NAME, name()).addTo(nameSpan);
		button(t("simplify name"), Map.of(ACTION,ACTION_AUTO,ROUTE,id().toString())).addTo(nameSpan);
		formInputs.add(t("Name"),nameSpan);
		Checkbox checkbox = new Checkbox(DISABLED, t("disabled"), disabled);
		if (disabled) checkbox.clazz("disabled");
		formInputs.add(t("State"),checkbox);		
		
		postForm.add(basicProperties());
		if (!turnouts.isEmpty()) postForm.add(turnouts());
		postForm.add(brakeTimes());
		Window win = super.properties(preForm, formInputs, postForm);
		previewScript().addTo(win);
		return win;
	}

	public boolean reactivate(Contact contact) {
		return triggeredContacts.remove(contact);
	}
	
	@Override
	public BaseClass remove() {
		LOG.debug("Removing route ({}) {}",id(),this);
		if (isSet(train)) train.removeChild(this);
		for (Tile tile : path) {
			tile.removeChild(this);
		}
		conditions.remove();
		for (String key : new Vector<String>(triggeredActions.keySet())){
			ActionList actionList = triggeredActions.remove(key);
			if (isSet(actionList)) actionList.remove();			
		};
		return super.remove();
	}

	@Override
	public void removeChild(BaseClass child) {
		conditions.remove(child);
		contacts.remove(child);
		if (child == endBlock) endBlock = null;
		path.remove(child);
		signals.remove(child);
		if (child == train) train = null;
		for (ActionList list : triggeredActions.values()) {
			list.removeChild(child);
		}
		turnouts.remove(child);
		if (child == startBlock) startBlock = null;
		triggeredContacts.remove(child);
		super.removeChild(child);
	}
	
	public boolean reset() {
		LOG.debug("{}.reset()",this);
		free();
		
		if (isSet(brakeProcessor)) brakeProcessor.abort();
		if (isSet(train)) {
			train.set(startBlock);
			train.heading(startDirection);
			if (train.route() == this) train.route(null);
			train = null;
		}
		state = State.FREE;
		return true;
	}
	
	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		file.write("{\""+ROUTES+"\":[\n");
		int count = 0;
		List<Route> routes = BaseClass.listElements(Route.class);
		for (Route route : routes) {			
			file.write(route.json().toString());
			if (++count < routes.size()) file.write(",");
			file.write("\n");
		}
		file.write("]}");
		file.close();
	}
	
	public Context set(Context newContext) {
		LOG.debug("{}.set({})",this,newContext);
		context = newContext;
		context.route(this);
		return context;
	}

	public void setLast(Turnout.State state) {
		if (isNull(state) || state == Turnout.State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) addTurnout((Turnout) lastTile,state);
	}
	
	public boolean setSignals(String state) {
		LOG.debug("{}.setSignals({})",this,state);
		for (Signal signal : signals) {
			if (!signal.state(isNull(state) ? Signal.GREEN : state)) return false;
		}
		return true;
	}
	
	public String shortName() {
		String[] parts = name().split("-");
		return parts[0].trim()+"–"+parts[parts.length-1].trim();
	}
	
	public Route simplyfyName() {
		String[] parts = name().split("-");
		if (parts.length>1) name(parts[0]+" - "+parts[parts.length-1]);
		return this;
	}

	public Route.State state(){
		return state;
	}
	
	public boolean start(Train newTrain) {
		if (state == State.STARTED) return true;
		LOG.debug("{}.start()",this);
		if (isNull(newTrain)) return false; // can't set route's train to null
		if (isSet(train)) {
			if (newTrain != train) return false; // can't alter route's train
		} else train = newTrain; // set new train 
		ActionList startActions = triggeredActions.get(ROUTE_START);
		if (isSet(startActions) && !startActions.fire(context,this+".start("+train.name()+")")) return false; // start actions failed
		state = State.STARTED;
		triggeredContacts.clear();
		return true;
	}

	public Block startBlock() {
		return startBlock;
	}
	
	public Integer startSpeed() {
		LOG.debug("{}.startSpeed()",this);
		ActionList startActions = triggeredActions.get(ROUTE_START);
		Context context = new Context(this);
		return isSet(startActions) ? startActions.getSpeed(context) : null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+(isSet(train)?train+":":"")+name()+")";
	}
	
	public void traceTrainFrom(Tile newHead) {
		LOG.debug("{}.traceTrainFrom({})",this,newHead);
		if (isNull(train)) return;
		if (newHead instanceof BlockContact) newHead = (Tile) ((BlockContact)newHead).parent();
		
		Tile traceHead = train.traceHead();
		Integer remainingLength = null;
		LinkedList<Tile> newTrace = new LinkedList<Tile>();
		for (int i=path.size(); i>0; i--) { // pfad rückwärts ablaufen
			Tile tile = path.elementAt(i-1);
			if (isNull(remainingLength)) {
				if (tile == newHead) traceHead = newHead; // wenn wir zuerst newHead finden: newHead als neuen traceHead übernehmen
				if (tile == traceHead) {
					remainingLength = train.length(); // sobald wir auf den traceHead stoßen: suche beenden
				}
			}		
			if (isSet(remainingLength)) {
				if (remainingLength>=0) {
					newTrace.add(tile);
					remainingLength -= tile.length();
				} else if (Route.freeBehindTrain) {
					try {
						tile.unset(this);				
					} catch (IllegalArgumentException e) {}
				} else break;				
			}
		}		
		train.setTrace(newTrace);
	}
	
	public Train train() {
		return train;
	}
	
	private Fieldset turnouts() {
		Fieldset win = new Fieldset(t("Turnouts"));
		Tag list = new Tag("ul");
		for (Entry<Turnout, Turnout.State> entry : turnouts.entrySet()) {
			Turnout turnout = entry.getKey();
			Plan.addLink(turnout, turnout+": "+t(entry.getValue().toString()), list);
		}
		list.addTo(win);
		return win;
	}
	
	protected Object update(HashMap<String, String> params,Plan plan) {
		LOG.debug("update({})",params);
		String name = params.get(NAME);
		if (isSet(name)) name(name);
		
		disabled = "on".equals(params.get(DISABLED));
		
		Condition condition = Condition.create(params.get(REALM_CONDITION));
		if (isSet(condition)) {
			condition.parent(this);
			conditions.add(condition);
		}
		super.update(params);
		return properties();
	}
}
