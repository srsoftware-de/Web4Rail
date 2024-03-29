package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

/**
 * Base class for all kinds of Blocks
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Block extends StretchableTile{
	protected static Logger LOG = LoggerFactory.getLogger(Block.class);
	
	private class TrainList implements Iterable<Train>{

		private LinkedList<Train> trains = new LinkedList<Train>();
		
		public void add(Train train) {
			trains.remove(train);
			trains.addFirst(train);
		}
		
		public boolean contains(Train t) {
			return trains.contains(t);
		}

		public Train first() {
			return trains.isEmpty() ? null : trains.getFirst();
		}

		public boolean isEmpty() {
			return trains.isEmpty();
		}

		@Override
		public Iterator<Train> iterator() {
			return trains.iterator();
		}
		
		public Train last() {
			return trains.isEmpty() ? null : trains.getLast();
		}
		
		public Vector<Train> list() {
			return new Vector<>(trains);
		}

		public boolean remove(BaseClass b) {
			return trains.remove(b);
		}
		
		@Override
		public String toString() {
			return trains.stream().map(t -> "["+t+"]→"+t.direction()).reduce((s1,s2) -> s1+", "+s2).orElse(t("empty"));
		}
	}
	private static final String ALLOW_TURN = "allowTurn";
	private static final String NAME       = "name";
	private static final String NO_TAG = "[default]";
	private static final String NEW_TAG = "new_tag";
	public static final String TAG = "tag";
	private static final String WAIT_TIMES = "wait_times";
	private static final String RAISE = "raise";
	public static final String ACTION_ADD_CONTACT = "add_contact";
	private static final String PARKED_TRAINS = "parked_trains";

	public String  name        = "Block";
	public boolean turnAllowed = false;
	private Vector<BlockContact> internalContacts = new Vector<BlockContact>();
	private TrainList parkedTrains = new TrainList();
	
	public Block() {
		super();
		WaitTime defaultWT = new WaitTime(NO_TAG);
		defaultWT.setMin(directionA(), 0);
		defaultWT.setMax(directionA(), 10000);
		defaultWT.setMin(directionB(), 0);
		defaultWT.setMax(directionB(), 10000);
		waitTimes.add(defaultWT);
		WaitTime learningWt = new WaitTime(t("learn"));
		learningWt.setMin(directionA(), 1000);
		learningWt.setMax(directionA(), 1000);
		learningWt.setMin(directionB(), 1000);
		learningWt.setMax(directionB(), 1000);
		waitTimes.add(learningWt);
	}
	
	/**
	 * aggregates all (directional) wait times for one tag
	 */
	public class WaitTime{		
		public String tag = "";
		private HashMap<Direction,Range> dirs = new HashMap<Direction, Range>();
		
		public WaitTime(String tag) {
			this.tag = tag;
		}

		public Range get(Direction dir) {
			Range range = dirs.get(dir);
			if (range == null) {
				range = new Range();
				dirs.put(dir, range);
			}
			return range;
		}		
		
		public JSONObject json() {
			JSONObject json = new JSONObject();
			json.put(TAG, tag);
			for (Entry<Direction, Range> entry : dirs.entrySet()) json.put(entry.getKey().toString(), entry.getValue().json());
			return json;
		}

		public WaitTime load(JSONObject json) {
			for (String key : json.keySet()) {
				if (key.equals(TAG)) {
					tag = json.getString(key);
				} else {
					Direction dir = Direction.valueOf(key);
					Range range = new Range().load(json.getJSONObject(key));
					dirs.put(dir, range);
				}
			}
			return this;
		}

		public WaitTime setMax(Direction dir,int max) {
			get(dir).max = max;
			return this;
		}

		public WaitTime setMin(Direction dir,int min) {
			get(dir).min = min;
			return this;
		}
		
		public WaitTime setTag(String newTag){
			tag = newTag;
			return this;
		}

		@Override
		public String toString() {
			return "WaitTime("+tag+", "+dirs+")";
		}

		public void validate() {
			for (Entry<Direction, Range> entry: dirs.entrySet()) entry.getValue().validate();			
		}
	}

	
	private Vector<WaitTime> waitTimes = new Vector<WaitTime>();
	
	public void addParkedTrain(Train train) {
		if (parkedTrains.contains(train)) return;
		if (isNull(train)) return;
		train.register();
		parkedTrains.add(train);
	}


	public Object addContact() {
		BlockContact contact = new BlockContact(this);
		plan.learn(contact);
		return t("Trigger contact to learn new contact");
	}
	
	public List<Route> arrivingRoutes() {
		return routes().stream().filter(route -> route.endBlock() == Block.this).collect(Collectors.toList());
	}

	
	@Override
	protected HashSet<String> classes() {
		HashSet<String> classes = super.classes();
		if (!parkedTrains.isEmpty()) classes.add(OCCUPIED);
		return classes;
	}
	
	@Override
	public Object click(boolean shift) throws IOException {
		Train train = occupyingTrain();
		return !shift && isSet(train) ? train.properties() : properties();
	}
	
	public int compareTo(Block other) {
		return name.compareTo(other.name);
	}
		
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		config.put(NAME, name);
		return config;
	}
	
	private Fieldset contactForm() {
		Fieldset fieldset = new Fieldset(t("internal contacts")).id("props-contacts");
		this.button(t("new contact"), Map.of(ACTION,ACTION_ADD_CONTACT)).addTo(fieldset);
		if (!internalContacts.isEmpty()) {
			Tag ul = new Tag("ul");
			for (BlockContact contact : internalContacts) {
				Tag li = contact.link("span", contact, contact.id().toString()).content(NBSP).addTo(new Tag("li"));
				contact.button(t("learn"),Map.of(ACTION,ACTION_ANALYZE)).addTo(li);
				contact.button(t("delete"),Map.of(ACTION,ACTION_DROP)).addTo(li);
				li.addTo(ul);
			
			}
			ul.addTo(fieldset);
		}
		return fieldset;
	}
	
	public Collection<? extends Contact> contacts() {
		return internalContacts;
	}
	
	public abstract Direction enterDirection(String dest);
	
	public abstract Direction directionA();
	public abstract Direction directionB();
	
	private Tile drop(String tag) {
		for (int i=0; i<waitTimes.size(); i++) {
			if (waitTimes.get(i).tag.equals(tag)) {
				waitTimes.remove(i);
				break;
			}
		}
		return this;
	}
	
	public void dropTrain(Train t) {
		parkedTrains.remove(t);
		plan.place(this);
	}
	
	@Override
	public boolean free(Train oldTrain) {
		parkedTrains.remove(oldTrain);
		if (!super.free(oldTrain)) return false;
		return true;
	}
	
	private WaitTime getWaitTime(String tag) {
		if (tag == null) return null;
		for (WaitTime wt : waitTimes) {
			if (wt.tag.equals(tag)) return wt;
		}
		return null;
	}

	public Range getWaitTime(Train train,Direction dir) {
		LOG.debug("{}.getWaitTime({},{})",this,train,dir);
		for (WaitTime wt : waitTimes) {
			LOG.debug("examinig {}",wt);
			if (train.tags().contains(wt.tag)) {
				LOG.info(t("{} @ {} using rule for \"{}\".",train,this,wt.tag));
				return wt.get(dir);
			}
		}
		LOG.info(t("{} @ {} using rule for \"[default]\".",train,this));
		return getWaitTime(NO_TAG).get(dir);
	}
	
	public int indexOf(BlockContact contact) {
		return 1+internalContacts.indexOf(contact);
	}
	
	@Override
	public boolean isFreeFor(Context context) {
		if (!super.isFreeFor(context)) return false;
		Train train = context.train();		
		if (isSet(occupyingTrain()) && occupyingTrain() == train) return true;
		return parkedTrains.isEmpty() || parkedTrains.contains(train) ||  train.isShunting(); 
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(ALLOW_TURN, turnAllowed);
		JSONArray jWaitTimes = new JSONArray();
		waitTimes.forEach(wt -> jWaitTimes.put(wt.json()));
		json.put(WAIT_TIMES, jWaitTimes);
		JSONObject jContacts = null;
		for (BlockContact contact : internalContacts) {
			if (contact.addr() != 0) {
				if (isNull(jContacts)) jContacts = new JSONObject();
				jContacts.put(contact.id().toString(), contact.json());
			}
		}
		if (isSet(jContacts)) json.put(CONTACT, jContacts);
		json.remove(REALM_TRAIN); // is set by TRAINS field for blocks
		if (!parkedTrains.isEmpty()) {
			JSONArray jTrains = new JSONArray();
			for (Train train : parkedTrains) {
				JSONObject to = new JSONObject();
				to.put(ID, train.id());
//				Direction dir = parkedTrains.directionOf(train);
//				if (isSet(dir)) to.put(DIRECTION, dir.toString());
				jTrains.put(to);
			}
			json.put(PARKED_TRAINS, jTrains);
		}
		return json;
	}
	
	public Train lastTrain() {
		return parkedTrains.last();
	}

	public List<Route> leavingRoutes() {
		return routes().stream().filter(route -> route.startBlock() == Block.this).collect(Collectors.toList());
	}

	
	/**
	 * If arguments are given, the first is taken as content, the second as tag type.
	 * If no content is supplied, name is set as content.
	 * If no type is supplied, "span" is preset.
	 * @param args
	 * @return
	 */
	public Tag link(String...args) {
		String tx = args.length<1 ? name+NBSP : args[0];
		String type = args.length<2 ? "span" : args[1];
		return link(type, tx).clazz("link","block");
	}
	
	@Override
	public Tile load(JSONObject json) {
		name = json.has(NAME) ? json.getString(NAME) : "Block";
		turnAllowed = json.has(ALLOW_TURN) && json.getBoolean(ALLOW_TURN);
		if (json.has(WAIT_TIMES)) {
			waitTimes.clear();
			JSONArray wtArr = json.getJSONArray(WAIT_TIMES);
			wtArr.forEach(object -> {
				if (object instanceof JSONObject) waitTimes.add(new WaitTime(null).load((JSONObject) object));
			});
		}
		if (json.has(CONTACT)) {
			JSONObject jContact = json.getJSONObject(CONTACT);			
			for (String key : jContact.keySet()) {
				try {
					new BlockContact(this).load(jContact.getJSONObject(key));
				} catch (JSONException e) {}
			}
		}
		
		new LoadCallback() {			
			@Override
			public void afterLoad() {
				if (json.has(PARKED_TRAINS)) {
					JSONArray jParkedTrains = json.getJSONArray(PARKED_TRAINS);
					for (Object o : jParkedTrains) {
						if (o instanceof JSONObject) {
							JSONObject trainData = (JSONObject) o;
							Train train = BaseClass.get(Id.from(trainData));
							if (isSet(train)) parkedTrains.add(train.set(Block.this));
						}
					}
				}
			}
		};
		
		return super.load(json);
	}
	
	@Override
	public boolean lockFor(Context context, boolean downgrade) {
		Train newTrain = context.train();
		LOG.debug("{}.lockFor({})",this,newTrain);
		Train train = lockingTrain();
		if (newTrain == train || parkedTrains.isEmpty() || parkedTrains.contains(newTrain) || this == newTrain.currentBlock() || newTrain.isShunting()) return super.lockFor(context, downgrade);
		return false;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Name"),new Input(NAME, name));
		formInputs.add("",new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed));
		Train train = occupyingTrain();
		if (isNull(train)) train = parkedTrains.first();
		formInputs.add(t("Train"),Train.selector(train, null));
		postForm.add(contactForm());
		postForm.add(waitTimeForm());
		if (!parkedTrains.isEmpty()) postForm.add(trainList());
		return super.properties(preForm, formInputs, postForm,errors);
	}

	public Tile raise(String tag) {
		for (int i=1; i<waitTimes.size(); i++) {
			WaitTime wt = waitTimes.get(i);
			if (wt.tag.equals(tag)) {
				waitTimes.remove(i);
				waitTimes.insertElementAt(wt, i-1);
				break;
			}
		}
		return this;
	}
	
	@Override
	public boolean reserveFor(Context context) {
		Train newTrain = context.train();
		LOG.debug("{}.lockFor({})",this,newTrain);
		Train train = lockingTrain();
		if (newTrain == train || parkedTrains.isEmpty() || parkedTrains.contains(newTrain) || this == newTrain.currentBlock() || newTrain.isShunting()) return super.reserveFor(context);
		return false;
	}

	public BlockContact register(BlockContact contact) {
		internalContacts.add(contact);
		return contact;
	}
	
	@Override
	public void removeChild(BaseClass child) {
		super.removeChild(child);
		internalContacts.remove(child);
		if (parkedTrains.remove(child)) plan.place(this);		
	}
	
	public void removeContact(BlockContact blockContact) {
		internalContacts.remove(blockContact);
	}
	
	@Override
	public boolean setTrain(Train newTrain) {
		if (isDisabled()) return false;
		if (isNull(newTrain)) return false;
		Train occupyingTrain = occupyingTrain();
		if (isSet(occupyingTrain) && newTrain != occupyingTrain && newTrain.isShunting()) parkedTrains.add(occupyingTrain.dropTrace(false));
		return super.setTrain(newTrain);
	}
	
	public abstract List<Connector> startPoints();

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",name);
		Vector<String> trainNames = new Vector<String>();
		Train lockingTrain = occupyingTrain();
		if (isSet(lockingTrain)) trainNames.add(lockingTrain.directedName());
		for (Train train: parkedTrains) trainNames.add(train.directedName());
		if (!trainNames.isEmpty())replacements.put("%text%",String.join(" | ", trainNames));
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+" Block");
		return tag;
	}
	
	@Override
	public String title() {
		StringBuilder sb = new StringBuilder(name);
		sb.append(" @ (");
		sb.append(x);
		sb.append(", ");
		sb.append(y);
		sb.append(")");
		Train occupyingTrain = occupyingTrain();
		if (isSet(occupyingTrain)) sb.append(title(occupyingTrain));
		if (isSet(parkedTrains)) for (Train parked : parkedTrains.trains) sb.append(title(parked));
		return sb.toString();
	}

	@Override
	public String toString() {
		return name + " @ ("+x+","+y+")";
	}
	
	private Fieldset trainList() {
		Fieldset fieldset = new Fieldset(t("Trains"));
		Tag list = new Tag("ul");
		for (Train t : parkedTrains) {
			if (isSet(t)) t.link("li", t, null).addTo(list);
		}
		list.addTo(fieldset);
		return fieldset;
	}
	
	public Vector<Train> trains(){
		return parkedTrains.list();
	}
	
	@Override
	public Tile update(Params params) {		
		if (params.containsKey(NAME)) name=params.getString(NAME);
		if (params.containsKey(Train.class.getSimpleName())) {
			Id trainId = Id.from(params,Train.class.getSimpleName());
			if (trainId.equals(0)) { // remove first train
				free(occupyingTrain());
			} else {
				Train newTrain = Train.get(trainId);
				if (isSet(newTrain) && newTrain != occupyingTrain()) {
					free(occupyingTrain());
					newTrain.dropTrace(true);
					if (connections(newTrain.direction()).isEmpty()) newTrain.heading(null);
					newTrain.set(this);					
				}
				if (newTrain.currentBlock() != this) {
					newTrain.dropTrace(true);
					newTrain.set(this);					
				}
			}
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && "on".equals(params.get(ALLOW_TURN));
		
		return super.update(params);
	}
		
	public Tile updateTimes(Params params) throws IOException {
		String tag = params.getString(ACTION_DROP);
		if (isSet(tag)) return drop(tag);
		tag = params.getString(RAISE);
		if (isSet(tag)) return raise(tag);
		String newTag = params.getString(NEW_TAG);
		if (isSet(newTag)) {
			newTag = newTag.replace(" ", "_").trim();
			if (newTag.isEmpty()) newTag = null;
		}
		
		for (Entry<String, Object> entry:params.entrySet()) {
			String key = entry.getKey();
			Object val = entry.getValue();
			
			if (key.startsWith("max.") || key.startsWith("min.")) {
				String[] parts = key.split("\\.");
				boolean isMin = parts[0].equals("min");
				tag = parts[1].equals("new_tag") ? newTag : parts[1];
				if (isNull(tag)) continue;
				Direction dir = Direction.valueOf(parts[2]);
				
				WaitTime wt = getWaitTime(tag);
				if (wt == null) {
					wt = new WaitTime(tag);
					waitTimes.add(wt);
				}
				if (isMin) {
					wt.setMin(dir, Integer.parseInt(val.toString()));
				} else wt.setMax(dir, Integer.parseInt(val.toString()));
			}			
		}
		for (WaitTime wt: waitTimes) wt.validate();
				
		return this;
	}
	
	public Fieldset waitTimeForm() {
		Fieldset win = new Fieldset(t("Wait times")).id("props-times");
		Form form = new Form("train-wait-form");
		new Tag("h4").content(t("Stop settings")).addTo(win);
		new Input(REALM,REALM_PLAN).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_TIMES).hideIn(form);
		
		new Tag("div").content(t("Minimum and maximum times (in Miliseconds) trains with the respective tag have to wait in this block.")).addTo(form);
		
		Direction dA = directionA();
		Direction dB = directionB();
		
		Tag table = new Tag("table");
		Tag row = new Tag("tr");			
		new Tag("td").content(t("Direction")).addTo(row);
		new Tag("th").content(t("{}bound",dA)).attr("colspan", 2).addTo(row);
		new Tag("th").content(t("{}bound",dB)).attr("colspan", 2).addTo(row);
		new Tag("td").content("").addTo(row).addTo(table);
		
		row = new Tag("tr");			
		new Tag("th").content(t("Tag")).addTo(row);
		new Tag("th").content(t("min")).addTo(row);
		new Tag("th").content(t("max")).addTo(row);
		new Tag("th").content(t("min")).addTo(row);
		new Tag("th").content(t("max")).addTo(row);
		new Tag("th").content(t("Actions")).addTo(row).addTo(table);
		
		int count = 0;
		for (WaitTime wt : waitTimes) {
			count++;
			row = new Tag("tr");				
			new Tag("td").content(wt.tag).addTo(row);
			new Input("min."+wt.tag+"."+dA,wt.get(dA).min).numeric().addTo(new Tag("td")).addTo(row);
			new Input("max."+wt.tag+"."+dA,wt.get(dA).max).numeric().addTo(new Tag("td")).addTo(row);
			new Input("min."+wt.tag+"."+dB,wt.get(dB).min).numeric().addTo(new Tag("td")).addTo(row);
			new Input("max."+wt.tag+"."+dB,wt.get(dB).max).numeric().addTo(new Tag("td")).addTo(row);
			Tag actions = new Tag("td");
			Map<String, String> props = Map.of(REALM,REALM_PLAN,ID,id().toString(),ACTION,ACTION_TIMES);
			switch (count) {
				case 1: 
					actions.content(""); break;
				case 2: 
					new Button("-",merged(props,Map.of(ACTION_DROP,wt.tag))).addTo(actions);
					break;
				default: 
					new Button("↑",merged(props,Map.of(RAISE,wt.tag))).addTo(actions);
					new Button("-",merged(props,Map.of(ACTION_DROP,wt.tag))).addTo(actions);
			}
			actions.addTo(row).addTo(table);
			
		}

		WaitTime defaultWT = getWaitTime(NO_TAG);

		row = new Tag("tr");
		new Input(NEW_TAG,"").attr("placeholder", t("new tag")).addTo(new Tag("td")).addTo(row);
		new Input("min."+NEW_TAG+"."+dA,defaultWT.get(dA).min).numeric().addTo(new Tag("td")).addTo(row);
		new Input("max."+NEW_TAG+"."+dA,defaultWT.get(dA).max).numeric().addTo(new Tag("td")).addTo(row);
		new Input("min."+NEW_TAG+"."+dB,defaultWT.get(dB).min).numeric().addTo(new Tag("td")).addTo(row);
		new Input("max."+NEW_TAG+"."+dB,defaultWT.get(dB).max).numeric().addTo(new Tag("td")).addTo(row).addTo(table);
		
		table.addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);
		
		return win;
	}
}
