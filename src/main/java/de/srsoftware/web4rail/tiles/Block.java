package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;

/**
 * Base class for all kinds of Blocks
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Block extends StretchableTile{
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
	private Vector<Train> parkedTrains = new Vector<Train>();
	
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
	
	public void add(Train parkedTrain) {
		parkedTrain.register();
		parkedTrains.add(parkedTrain);
	}


	public Object addContact() {
		BlockContact contact = new BlockContact(this);
		plan.learn(contact);
		return t("Trigger contact to learn new contact");
	}
	
	@Override
	protected HashSet<String> classes() {
		HashSet<String> classes = super.classes();
		if (!parkedTrains.isEmpty()) classes.add(OCCUPIED);
		return classes;
	}
	
	@Override
	public Object click() throws IOException {
		if (isSet(train) && train.currentBlock() == this) return train.properties();
		return super.click();
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
		Fieldset fieldset = new Fieldset(t("internal contacts"));
		this.button(t("new contact"), Map.of(ACTION,ACTION_ADD_CONTACT)).addTo(fieldset);
		if (!internalContacts.isEmpty()) {
			Tag ul = new Tag("ul");
			for (BlockContact contact : internalContacts) {
				Tag li = contact.link("span", contact).content(NBSP).addTo(new Tag("li"));
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
	
	private WaitTime getWaitTime(String tag) {
		if (tag == null) return null;
		for (WaitTime wt : waitTimes) {
			if (wt.tag.equals(tag)) return wt;
		}
		return null;
	}

	public Range getWaitTime(Train train,Direction dir) {
		for (WaitTime wt : waitTimes) {
			if (train.tags().contains(wt.tag)) {
				LOG.info(t("{} using rule for \"{}\".",train,wt.tag));
				return wt.get(train.direction());
			}
		}
		return getWaitTime(NO_TAG).get(dir);
	}
	
	public int indexOf(BlockContact contact) {
		return 1+internalContacts.indexOf(contact);
	}
	
	@Override
	public boolean isFreeFor(Context context) {
		if (!super.isFreeFor(context)) return false;
		if (parkedTrains.isEmpty()) return true;
		Train t = isSet(context) ? context.train() : null;
		return isSet(t) ? t.isShunting() : false; // block contains train(s), thus it is olny free for shunting train
	}
		
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(ALLOW_TURN, turnAllowed);
		JSONArray jWaitTimes = new JSONArray();
		for (WaitTime wt : waitTimes) jWaitTimes.put(wt.json());
		json.put(WAIT_TIMES, jWaitTimes);
		JSONObject jContacts = null;
		for (BlockContact contact : internalContacts) {
			if (contact.addr() != 0) {
				if (isNull(jContacts)) jContacts = new JSONObject();
				jContacts.put(contact.id().toString(), contact.json());
			}
		}
		if (isSet(jContacts)) json.put(CONTACT, jContacts);
		if (!parkedTrains.isEmpty()) {
			JSONArray ptids = new JSONArray();
			for (Train parked : parkedTrains) {
				if (isSet(parked)) ptids.put(parked.id().toString());
			}
			json.put(PARKED_TRAINS, ptids);
		}
		return json;
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
				new BlockContact(this).load(jContact.getJSONObject(key));
			}
		}
		if (json.has(PARKED_TRAINS)) {
			JSONArray ptids = json.getJSONArray(PARKED_TRAINS);
			for (Object id : ptids) {
				Train train = BaseClass.get(new Id(id.toString()));
				if (isSet(train)) parkedTrains.add(train);
			}
		}
		return super.load(json);
	}
	
	private Fieldset parkedTrains() {
		Fieldset fieldset = new Fieldset(t("parked trains"));
		Tag list = new Tag("ul");
		for (Train t : parkedTrains) {
			if (isSet(t)) t.link("li", t).addTo(list);
		}
		list.addTo(fieldset);
		return fieldset;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Name"),new Input(NAME, name));
		formInputs.add("",new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed));
		formInputs.add(t("Train"),Train.selector(train, null));
		postForm.add(contactForm());
		postForm.add(waitTimeForm());
		if (!parkedTrains.isEmpty()) postForm.add(parkedTrains());
		return super.properties(preForm, formInputs, postForm);
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

	public BlockContact register(BlockContact contact) {
		internalContacts.add(contact);
		return contact;
	}
	
	@Override
	public void removeChild(BaseClass child) {
		super.removeChild(child);
		internalContacts.remove(child);
		parkedTrains.remove(child);
	}
	
	public void removeContact(BlockContact blockContact) {
		internalContacts.remove(blockContact);
	}
	
	public static Select selector(Object preset,Collection<Block> exclude) {
		Block preselected = preset instanceof Block ? (Block) preset : null;
		String firstEntry = preset instanceof String ? (String) preset : t("unset"); 
		if (isNull(exclude)) exclude = new Vector<Block>();
		Select select = new Select(Block.class.getSimpleName());
		new Tag("option").attr("value","0").content(firstEntry).addTo(select);
		List<Block> blocks = BaseClass.listElements(Block.class);
		Collections.sort(blocks, (b1,b2) -> b1.name.compareTo(b2.name));
		for (Block block : blocks) {
			if (exclude.contains(block)) continue;
			Tag opt = select.addOption(block.id(), block);
			if (block == preselected) opt.attr("selected", "selected");
		}
		return select;
	}

	public abstract List<Connector> startPoints();

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",name);
		Vector<String> trainNames = new Vector<String>();
		if (isSet(train)) trainNames.add(train.directedName());
		for (Train t:parkedTrains) {
			if (isSet(t)) trainNames.add(t.name());
		}
		if (!trainNames.isEmpty())replacements.put("%text%",String.join(" | ", trainNames));
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+" Block");
		return tag;
	}
	
	@Override
	public String title() {
		return name;
	}
	
	@Override
	public String toString() {
		return name + " @ ("+x+","+y+")";
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {		
		if (params.containsKey(NAME)) name=params.get(NAME);
		if (params.containsKey(Train.class.getSimpleName())) {
			Id trainId = Id.from(params,Train.class.getSimpleName());
			if (trainId.equals(0)) {
				if (isSet(train)) {
					train.dropTrace();
					train.set(null);
				}				
				train = null;
			} else {
				Train newTrain = Train.get(trainId);
				if (isSet(newTrain) && newTrain != train) {
					newTrain.dropTrace();
					if (connections(newTrain.direction()).isEmpty()) newTrain.heading(null);
					newTrain.set(this);
				}
			}
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		
		return super.update(params);
	}
		
	public Tile updateTimes(HashMap<String, String> params) throws IOException {
		String tag = params.get(ACTION_DROP);
		if (isSet(tag)) return drop(tag);
		tag = params.get(RAISE);
		if (isSet(tag)) return raise(tag);
		String newTag = params.get(NEW_TAG);
		if (isSet(newTag)) {
			newTag = newTag.replace(" ", "_").trim();
			if (newTag.isEmpty()) newTag = null;
		}
		
		for (Entry<String, String> entry:params.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			
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
					wt.setMin(dir, Integer.parseInt(val));
				} else wt.setMax(dir, Integer.parseInt(val));
			}			
		}
		for (WaitTime wt: waitTimes) wt.validate();
				
		return this;
	}
	
	public Fieldset waitTimeForm() {
		Fieldset win = new Fieldset(t("Wait times"));
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
