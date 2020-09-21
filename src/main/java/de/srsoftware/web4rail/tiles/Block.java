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
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name).addTo(label);
		label.addTo(form);

		new Checkbox(ALLOW_TURN,t("Turn allowed"),turnAllowed).addTo(form);
		
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
	
	public void train(Train train) throws IOException {
		if (train != null) train.block(this);
		super.train(train);
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(NAME)) name=params.get(NAME);
		turnAllowed = params.containsKey(ALLOW_TURN) && params.get(ALLOW_TURN).equals("on");
		return this;
	}	
}
