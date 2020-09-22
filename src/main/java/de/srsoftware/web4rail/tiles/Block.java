package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public abstract class Block extends StretchableTile{
	private static final String NAME = "name";
	public String name = "Block";
	
	private static final String ALLOW_TURN = "allowTurn";
	public boolean turnAllowed = false;
	
	private static final String TRAIN = "train";
	
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
			Train tr = Train.get(json.getLong(TRAIN)); 
			train(tr);
		}
		return this;
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		
		new Input(NAME, name).addTo(new Label(t("name:")+" ")).addTo(new Tag("p")).addTo(form);
		
		new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed).addTo(new Tag("p")).addTo(form);

		Tag select = new Tag("select").attr("name", TRAIN);
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Train train : Train.list()) {
			Tag opt = new Tag("option").attr("value", ""+train.id);
			if (this.train == train) opt.attr("selected", "selected");
			opt.content(train.toString()).addTo(select);
		}
		select.addTo(new Label(t("Trains:")+" ")).addTo(new Tag("p")).addTo(form);
		
		return form;
	}
	
	@Override
	public Tag propMenu() {
		Tag window = super.propMenu();
		
		if (train != null) {
			new Tag("h4").content(t("Train:")).addTo(window);
			train.link("span").addTo(window);
			new Tag("span").clazz("link").attr("onclick","train("+train.id+",'"+Train.MODE_START+"')").content(" - "+t("start")).addTo(window);
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
	public String toString() {
		return getClass().getSimpleName()+"("+name+") @ ("+x+","+y+")";
	}
	
	public void train(Train newTrain) throws IOException {
		if (train == newTrain) return;
		if (train != null) train.block(null); // vorherigen Zug rauswerfen
		if (newTrain != null) newTrain.block(this);
		super.train(newTrain);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		super.update(params);
		if (params.containsKey(NAME)) name=params.get(NAME);
		if (params.containsKey(TRAIN)) {
			long trainId = Long.parseLong(params.get(TRAIN));
			train(trainId == 0 ? null : Train.get(trainId));
		}
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		return this;
	}	
}
