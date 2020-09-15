package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Route;

public class BlockH extends Block{
	private static final String NAME = "name";
	Contact north,center,south;
	private String name = "Block";
	
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
	
	@Override
	public int len() {
		return length;
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name).addTo(label);		
		label.addTo(form);

		return form;
	}
	
	@Override
	public Set<Route> routes() {		
		return null;
	}
	
	@Override
	public List<Connector> startPoints() {
		return List.of(new Connector(x-1, y, Plan.EAST),new Connector(x+len(), y, Plan.WEST));
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%text%",name);
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
}
