package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Route;

public abstract class Block extends StretchableTile{
	private static final String NAME = "name";
	public String name = "Block";
	private HashSet<Route> routes = new HashSet<Route>();
	
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

	public Set<Route> routes(){
		return routes;
	}

	public abstract List<Connector> startPoints();

	public void add(Route route) {
		routes.add(route);
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name).addTo(label);		
		label.addTo(form);
		
		new Tag("h4").content(t("Routes from here:")).addTo(form);
		Tag routeList = new Tag("ul");
		for (Route route : routes) {
			new Tag("li").content(route.id()).addTo(routeList);
		}
		routeList.addTo(form);

		return form;
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
