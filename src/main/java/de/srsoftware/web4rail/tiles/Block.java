package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.moving.Train.WaitTime;
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
	public String  name        = "Block";
	public boolean turnAllowed = false;
		
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
		return json;
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		name = json.has(NAME) ? json.getString(NAME) : "Block";
		turnAllowed = json.has(ALLOW_TURN) && json.getBoolean(ALLOW_TURN);
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
			
			Tag table = new Tag("table");
			Tag row = new Tag("tr");
			new Tag("td").content(t("Direction")).addTo(row);
			new Tag("th").attr("colspan", 2).content(directionA().toString()).addTo(row);
			new Tag("th").attr("colspan", 2).content(directionB().toString()).addTo(row);
			
			row.addTo(table);
			
			row = new Tag("tr");
			new Tag("th").content(t("Train")).addTo(row);
			new Tag("th").content(t("min")).addTo(row);
			new Tag("th").content(t("max")).addTo(row);
			new Tag("th").content(t("min")).addTo(row);
			new Tag("th").content(t("max")).addTo(row);
			row.addTo(table);

			for (Train train : Train.list()) {
				row = new Tag("tr");
				new Tag("td").content(train.name()).addTo(row);
				Direction a = directionA();
				WaitTime wtA = train.waitTime(this, a);
				Direction b = directionB();
				WaitTime wtB = train.waitTime(this, b);
				new Input("train."+train.id+"."+directionA()+".min",wtA.min).numeric().addTo(new Tag("td")).addTo(row);
				new Input("train."+train.id+"."+directionA()+".max",wtA.max).numeric().addTo(new Tag("td")).addTo(row);
				new Input("train."+train.id+"."+directionB()+".min",wtB.min).numeric().addTo(new Tag("td")).addTo(row);
				new Input("train."+train.id+"."+directionB()+".max",wtB.max).numeric().addTo(new Tag("td")).addTo(row);
				row.addTo(table);
			}
			
			table.addTo(form);
			
			new Button(t("Apply")).addTo(form).addTo(win);
			
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
		
		for (Entry<String, String> entry :  params.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("train.")) {
				String[] parts = key.split("\\.");
				int trainId = Integer.parseInt(parts[1]);
				Train t = Train.get(trainId);
				if (t == null) continue;

				Direction dir = Direction.valueOf(parts[2]);
				boolean min = parts[3].equals("min");
				int time = Integer.parseInt(entry.getValue());
				
				t.setWaitTime(this,dir,time,min);
				
				LOG.debug("{} / {} : {}",t,dir,t.waitTime(this, dir));
				
			}
		}
		
		return super.update(params);
	}
}
