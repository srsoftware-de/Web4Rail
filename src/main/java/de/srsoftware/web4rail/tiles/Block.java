package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public abstract class Block extends StretchableTile{
	private static final String NAME = "name";
	public String name = "Block";
	
	private static final String ALLOW_TURN = "allowTurn";
	public boolean turnAllowed = false;
	
	private static final String TRAIN = Train.class.getSimpleName();
	
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		config.put(NAME, name);
		return config;
	}
	
	@Override
	public boolean free() {
		return train == null && super.free();
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(ALLOW_TURN, turnAllowed);
		if (train != null) json.put(TRAIN, train.id);
		return json;
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		name = json.has(NAME) ? json.getString(NAME) : "Block";
		turnAllowed = json.has(ALLOW_TURN) && json.getBoolean(ALLOW_TURN);
		if (json.has(TRAIN)) {
			Train tr = Train.get(json.getInt(TRAIN)); 
			train(tr);
		}
		return this;
	}
	
	@Override
	public Tag propForm(String id) {
		Tag form = super.propForm(id);
		new Tag("h4").content(t("Block properties")).addTo(form);
		
		new Input(NAME, name).addTo(new Label(t("name:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed).addTo(new Tag("p")).addTo(form);

		Select select = Train.selector(train, null);
		select.addTo(new Label(t("Train:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		return form;
	}
	
	@Override
	public Tag propMenu() {
		Tag window = super.propMenu();
		
		if (train != null) {
			window.children().insertElementAt(new Button(t("stop"),"train("+train.id+",'"+ACTION_STOP+"')"), 1);
			window.children().insertElementAt(new Button(t("start"),"train("+train.id+",'"+ACTION_START+"')"), 1);
			window.children().insertElementAt(train.link("span"), 1);
			window.children().insertElementAt(new Tag("h4").content(t("Train:")), 1);
		}
		return window;
	}
	
	public abstract List<Connector> startPoints();

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%text%",train == null ? name : train.name());
		Tag tag = super.tag(replacements);
		if (train != null) tag.clazz(tag.get("class")+" occupied");
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
	
	public Tile train(Train newTrain) throws IOException {
		if (train == newTrain) return this;
		if (train != null) train.block(null); // vorherigen Zug rauswerfen
		if (newTrain != null) newTrain.block(this);
		return super.train(newTrain);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {		
		if (params.containsKey(NAME)) name=params.get(NAME);
		if (params.containsKey(TRAIN)) {
			int trainId = Integer.parseInt(params.get(TRAIN));
			Train t = Train.get(trainId);
			if (t != null) {
				Block oldBlock = t.block();
				if (oldBlock != null) oldBlock.train(null);
				train(t);
			}			
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		return super.update(params);
	}	
}
