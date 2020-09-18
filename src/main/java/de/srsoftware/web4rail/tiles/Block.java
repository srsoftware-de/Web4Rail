package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.moving.Train;

public abstract class Block extends StretchableTile{
	private static final String NAME = "name";
	public String name = "Block";
	private Train train;
	
	@Override
	public JSONObject config() {
		JSONObject config = super.config();
		config.put(NAME, name);
		return config;
	}
	
	@Override
	public void configure(JSONObject config) {
		super.configure(config);
		if (config.has(NAME)) name = config.getString(NAME);
	}

	public abstract List<Connector> startPoints();

	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name).addTo(label);		
		label.addTo(form);
		
		return form;
	}
	
	@Override
	public Tag propMenu() {
		Tag window = super.propMenu();
		
		if (train != null) {
			new Tag("h4").content(t("Train:")).addTo(window);
			new Tag("span").clazz("link").attr("onclick","train("+x+","+y+",'show')").content(train.name()).addTo(window);
			new Tag("span").clazz("link").attr("onclick","train("+x+","+y+",'start')").content(" - "+t("start")).addTo(window);
		}
		return window;
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%text%",train == null ? name : train.name());
		return super.tag(replacements);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+name+") @ ("+x+","+y+")";
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		super.update(params);
		if (params.containsKey(NAME)) name=params.get(NAME);
		return this;
	}

	public void setTrain(Train train) {
		this.train = train;
	}

	public Train train() {
		return train;
	}
}
