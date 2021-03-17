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
import de.srsoftware.web4rail.threads.BrakeProcess;
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
	
/*	public enum State {
		FREE, LOCKED, PREPARED, STARTED;
	}*/
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
	//private State state = State.FREE;
	public static boolean freeBehindTrain = true;

	private static final String ROUTE_START = "route_start";

	private static final String ROUTE_SETUP = "route_setup";

	private static HashMap<Id, String> names = new HashMap<Id, String>(); // maps id to name. needed to keep names during plan.analyze()

	private HashMap<String,Integer>        brakeTimes = new HashMap<String, Integer>();
	private ConditionList                  conditions;
	private Vector<Contact>                contacts;
	private Context						   context;
	private boolean                        disabled = false;
	private Block                          endBlock = null;
	public  Direction					   endDirection;
	private Vector<Tile>                   path;
	private Vector<Signal>                 signals;
	private HashMap<String,ActionList>     triggeredActions = new HashMap<String, ActionList>();
	private HashMap<Turnout,Turnout.State> turnouts;
	private Block                          startBlock = null;
	public  Direction 					   startDirection;
	private HashSet<Contact>			   triggeredContacts = new HashSet<>();

	private Route nextPreparedRoute;
	
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
		
//		if (isSet(train)) train.link("span",t("Train")+": "+train).addTo(fieldset);
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
		Integer result = brakeTimes.get(brakeId);
		Collection<Integer> values = brakeTimes.values();
		return values.isEmpty() ? BrakeProcess.defaultTimeStep : values.stream().mapToInt(Integer::intValue).sum()/values.size();
	}

	public void brakeTime(String brakeId, Integer newTimeStep) {
		LOG.debug("new brake time for route {}: {}",this,newTimeStep);
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
			
			int count = 0;
			for (int i=0;i<contacts.size();i++) { // chose second contact, that is not a BlockContact
				Contact contact = contacts.get(i);
				if (contact instanceof BlockContact) continue;
				if (count++==1) { // second contact, that is not a BlockContact:
					for (Signal signal : signals) add(contact.trigger(),new SetSignal(this).set(signal).to(Signal.RED));
					break;
				}
			}
			add(trigger,new PreserveRoute(this));

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
	 * @return 
	 */
	public Context contact(Contact contact) {
		context.contact(contact);
		if (triggeredContacts.contains(contact)) return context; // don't trigger contact a second time
		triggeredContacts.add(contact);
		LOG.debug("{} on {} activated {}.",context.train(),this,contact);
		ActionList actions = triggeredActions.get(contact.trigger());
		LOG.debug("Contact has id {} / trigger {} and is assigned with {}",contact.id(),contact.trigger(),isNull(actions)?t("nothing"):actions);
		if (isNull(actions)) return context;
		actions.fire(context,"Route.Contact("+contact.addr()+")");
		Context previousContext = context;
		if (context.invalidated()) context = null; // route has been freed in between.
		return previousContext;
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
	
	public void dropBraketimes(String...brakeIds) {
		for (String brakeId : brakeIds) brakeTimes.remove(brakeId);
	}
	
	public Route dropNextPreparedRoute() {
		try {
			return nextPreparedRoute;
		} finally {
			nextPreparedRoute = null;
		}
	}
	
	public Block endBlock() {
		return endBlock;
	}	
	
	public void finish(Train train) {
		LOG.debug("{}.finish()",this);
		train.endRoute(endBlock,endDirection);
		free();				
		train = null;
	}
	
	private void free() {
		context.invalidate(); // do not set to null: 
		// this action may be called from route.contact → finishRoute, which calls train.updateTrace afterwards, which in turn requires context
		Train train = context.train();
		Vector<Tile> reversedPath = reverse(path());
		for (Tile tile : reversedPath) {
			if (isSet(train) && !train.onTrace(tile)) tile.free(train);
		}
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
	
	public Route getNextPreparedRoute() {
		return nextPreparedRoute;
	}
	
	public Id id() {
		if (isNull(id)) id = new Id(""+(generateName().hashCode()));
		return id;
	}
	
	public boolean isDisabled() {
		return disabled;
	}
		
	public boolean isFreeFor(Context train) {
		LOG.debug("{}.isFreeFor({})",this,train);
		if (isNull(train.train())) return false;
		for (Tile tile : path) {			
			if (!tile.isFreeFor(train)) return false;
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
		if (json.has("action_lists")) { // Legacy
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
		if (json.has("conditions")) { // Legacy
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
		if (json.has(SETUP_ACTIONS)) { // Legacy
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
		if (json.has(START_ACTIONS)) { // Legacy
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
		return isSet(path) ? new Vector<>(path) : new Vector<>();
	}
	
	public boolean prepareAndLock() {
		LOG.debug("{}.prepareAndLock()",this);
		Train train = context.train();
		ActionList setupActions = triggeredActions.get(ROUTE_SETUP);
		if (isSet(setupActions) && !setupActions.fire(context.route(this),this+".prepare()")) {
			LOG.debug("Was not able to prepare route for {}.",train);
			return false;
		}

		for (Tile tile : path) {
			if (context.invalidated() || !tile.lockFor(context,false)) {
				LOG.debug("Was not able to allocate route for {}.",context);				
				return false;
			}
		}
		return true;
	}
	
	private Tag previewScript() {
		Tag script = new Tag("script").attr("type", "text/javascript");
		for (Tile tile : path) {
			script.content("$('#"+tile.id()+"').addClass('preview');\n");
		}
		return script;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {

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
		Window win = super.properties(preForm, formInputs, postForm,errors);
		previewScript().addTo(win);
		return win;
	}

	public boolean reactivate(Contact contact) {
		return triggeredContacts.remove(contact);
	}
	
	@Override
	public BaseClass remove() {
		LOG.debug("Removing route ({}) {}",id(),this);
//		if (isSet(train)) train.removeChild(this);
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
//		if (child == train) train = null;
		for (ActionList list : triggeredActions.values()) {
			list.removeChild(child);
		}
		turnouts.remove(child);
		if (child == startBlock) startBlock = null;
		triggeredContacts.remove(child);
		super.removeChild(child);
	}
	
	public boolean reserveFor(Context newContext) {
		LOG.debug("{}.reserverFor({})",this,newContext);
		if (isSet(context)) return false; // route already has context!
		context = newContext;
		for (Tile tile : path) {
			if (newContext.invalidated() || !tile.reserveFor(newContext)) {
				LOG.debug("Was not able to allocate route for {}.",newContext);				
				return false;
			}
		}
		return true;
	}
	
	public boolean reset() {
		LOG.debug("{}.reset()",this);
		setSignals(Signal.RED);
		Train train = context.train();
		free();
		train.drop(this);
		context = null;
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
	
	public void setLast(Turnout.State state) {
		if (isNull(state) || state == Turnout.State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) addTurnout((Turnout) lastTile,state);
	}
	
	public void setNextPreparedRoute(Route route) {
		nextPreparedRoute = route;
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
		if (parts.length>1) name(parts[0].trim()+" - "+parts[parts.length-1].trim());
		return this;
	}

	public boolean start() {
		LOG.debug("{}.start()",this);
		if (isNull(context) || context.invalidated()) return false;
		
		Train train = context.train();
		if (isNull(train)) return false; // can't set route's train to null
		train.setRoute(this); // set new train
		
		triggeredContacts.clear();

		ActionList startActions = triggeredActions.get(ROUTE_START);
		
		if (isSet(startActions)) {
			context.route(this);
			String cause = this+".start("+train.name()+")";
			if (!startActions.fire(context,cause)) return false; // start actions failed
		}

		context.waitTime(endBlock.getWaitTime(train, endDirection).random());
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
		return getClass().getSimpleName()+"("+name()+")";
	}
		
/*	public Train train() {
		return train;
	}*/
	
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

	public Integer waitTime() {
		return isNull(context) ? null : context.waitTime();
	}
}
