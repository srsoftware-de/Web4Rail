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
	private Train trailingTrain = null;
		
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		config.put(NAME, name);
		return config;
	}
	
	@Override
	public boolean isFree() {
		return super.isFree() && trailingTrain == null;
	}
		
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
	public Tag propForm(String id) {
		Tag form = super.propForm(id);
		new Tag("h4").content(t("Block properties")).addTo(form);
		
		new Input(NAME, name).addTo(new Label(t("name:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed).addTo(new Tag("p")).addTo(form);

		Select select = Train.selector(trainHead, null);
		select.addTo(new Label(t("Train:")+NBSP)).addTo(new Tag("p")).addTo(form);
		
		return form;
	}
	
	@Override
	public Tag propMenu() {
		Tag window = super.propMenu();
		
		if (trainHead != null) {
			window.children().insertElementAt(new Button(t("stop"),"train("+trainHead.id+",'"+ACTION_STOP+"')"), 1);
			window.children().insertElementAt(new Button(t("start"),"train("+trainHead.id+",'"+ACTION_START+"')"), 1);
			window.children().insertElementAt(trainHead.link("span"), 1);
			window.children().insertElementAt(new Tag("h4").content(t("Train:")), 1);
		}
		return window;
	}
	
	public abstract List<Connector> startPoints();

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%text%",name);
		if (trailingTrain != null) replacements.put("%text%","("+trailingTrain.name()+")");
		if (trainHead != null) replacements.put("%text%",trainHead.directedName());
		Tag tag = super.tag(replacements);
		if (trainHead != null || trailingTrain != null) tag.clazz(tag.get("class")+" occupied");
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
	
	public void trailingTrain(Train train) {
		trailingTrain = train;
		this.trainHead = null;
		plan.place(this);
	}

	public Train trailingTrain() {
		return trailingTrain;
	}

	@Override
	public void unlock() {
		trailingTrain = null;
		super.unlock();
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {		
		if (params.containsKey(NAME)) name=params.get(NAME);
		if (params.containsKey(Train.HEAD)) {
			int trainId = Integer.parseInt(params.get(Train.HEAD));
			if (trainId == 0) {
				trainHead(null);
			} else {
				Train t = Train.get(trainId);
				if (t != null) trainHead = t.block(this,true);
			}
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		return super.update(params);
	}
}
