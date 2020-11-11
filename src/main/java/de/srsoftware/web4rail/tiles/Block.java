package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
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
	private static final String TAG = "tag";
	private static final String WAIT_TIMES = "wait_times";

	public String  name        = "Block";
	public boolean turnAllowed = false;
	
	public Block() {
		super();
		WaitTime defaultWT = new WaitTime(NO_TAG);
		defaultWT.setMin(directionA(), 0);
		defaultWT.setMax(directionA(), 10000);
		defaultWT.setMin(directionB(), 0);
		defaultWT.setMax(directionB(), 10000);
		waitTimes.add(defaultWT);
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
	}

	
	private Vector<WaitTime> waitTimes = new Vector<WaitTime>();
		
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		config.put(NAME, name);
		return config;
	}
	
	public abstract Direction directionA();
	public abstract Direction directionB();
		
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(ALLOW_TURN, turnAllowed);
		JSONArray jWaitTimes = new JSONArray();
		for (WaitTime wt : waitTimes) jWaitTimes.put(wt.json());
		json.put(WAIT_TIMES, jWaitTimes);
		return json;
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		name = json.has(NAME) ? json.getString(NAME) : "Block";
		turnAllowed = json.has(ALLOW_TURN) && json.getBoolean(ALLOW_TURN);
		if (json.has(WAIT_TIMES)) {
			waitTimes.clear();
			JSONArray wtArr = json.getJSONArray(WAIT_TIMES);
			wtArr.forEach(object -> {
				if (object instanceof JSONObject) waitTimes.add(new WaitTime(null).load((JSONObject) object));
			});
		}
		return this;
	}
	
	@Override
	public Form propForm(String id) {
		Form form = super.propForm(id);
		new Tag("h4").content(t("Block properties")).addTo(form);
		
		new Input(NAME, name).addTo(new Label(t("name:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed).addTo(new Tag("p")).addTo(form);

		Select select = Train.selector(train, null);
		select.addTo(new Label(t("Train:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		return form;
	}
	
	@Override
		public Window propMenu() {
			Window win = super.propMenu();
			Form form = new Form("train-wait-form");
			new Tag("h4").content(t("Stop settings")).addTo(win);
			new Input(REALM,REALM_PLAN).hideIn(form);
			new Input(ID,id()).hideIn(form);
			new Input(ACTION,ACTION_UPDATE).hideIn(form);
			
			Direction dA = directionA();
			Direction dB = directionB();
			
			Tag table = new Tag("table");
			Tag row = new Tag("tr");			
			new Tag("td").content(t("Direction")).addTo(row);
			new Tag("th").content(t("{}",dA)).attr("colspan", 2).addTo(row);
			new Tag("th").content(t("{}",dB)).attr("colspan", 2).addTo(row).addTo(table);
			
			row = new Tag("tr");			
			new Tag("th").content(t("Tag")).addTo(row);
			new Tag("th").content(t("min")).addTo(row);
			new Tag("th").content(t("max")).addTo(row);
			new Tag("th").content(t("min")).addTo(row);
			new Tag("th").content(t("max")).addTo(row).addTo(table);
			
			for (WaitTime wt : waitTimes) {
				row = new Tag("tr");
				new Tag("td").content(wt.tag).addTo(row);
				new Input("min."+wt.tag+"."+dA,wt.get(dA).min).numeric().addTo(new Tag("td")).addTo(row);
				new Input("max."+wt.tag+"."+dA,wt.get(dA).max).numeric().addTo(new Tag("td")).addTo(row);
				new Input("min."+wt.tag+"."+dB,wt.get(dB).min).numeric().addTo(new Tag("td")).addTo(row);
				new Input("max."+wt.tag+"."+dB,wt.get(dB).max).numeric().addTo(new Tag("td")).addTo(row).addTo(table);
				
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
		
	public abstract List<Connector> startPoints();

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",name);
		if (isSet(train)) replacements.put("%text%",train.directedName());
		Tag tag = super.tag(replacements);
		if (isSet(train)) tag.clazz(tag.get("class")+" occupied");
		return tag;
	}
	
	@Override
	public String title() {
		return name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+name+") @ ("+x+","+y+")";
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {		
		if (params.containsKey(NAME)) name=params.get(NAME);
		if (params.containsKey(Train.class.getSimpleName())) {
			int trainId = Integer.parseInt(params.get(Train.class.getSimpleName()));
			if (trainId == 0) {
				if (isSet(train)) train.dropTrace();
				train = null;
			} else {
				Train newTrain = Train.get(trainId);
				if (isSet(newTrain) && newTrain != train) {
					newTrain.dropTrace();
					if (connections(newTrain.direction()).isEmpty()) {
						newTrain.heading(null);
					}
					newTrain.set(this);
				}
			}
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		
		String newTag = params.get(NEW_TAG);
		for (Entry<String, String> entry:params.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			
			if (key.startsWith("max.") || key.startsWith("min.")) {
				String[] parts = key.split("\\.");
				boolean isMin = parts[0].equals("min");
				String tag = parts[1].equals("new_tag") ? newTag : parts[1];
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
				
		return super.update(params);
	}

	private WaitTime getWaitTime(String tag) {
		if (tag == null) return null;
		for (WaitTime wt : waitTimes) {
			if (wt.tag.equals(tag)) return wt;
		}
		return null;
	}

	public Range getWaitTime(Train train) {
		for (WaitTime wt : waitTimes) {
			if (train.tags().contains(wt.tag)) {
				return wt.get(train.direction());
			}
		}
		return getWaitTime(NO_TAG).get(train.direction());
	}
}
