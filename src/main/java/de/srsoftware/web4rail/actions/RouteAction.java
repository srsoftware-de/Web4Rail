package de.srsoftware.web4rail.actions;

import org.json.JSONObject;

import de.srsoftware.web4rail.Route;

public abstract class RouteAction extends Action {

	private static final String ROUTE = "route";
	protected Route route;
	
	public RouteAction(Route route) {
		this.route = route;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(ROUTE, route.id());
		return json;
	}
}
