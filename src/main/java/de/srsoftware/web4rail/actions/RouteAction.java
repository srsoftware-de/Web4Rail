package de.srsoftware.web4rail.actions;

import org.json.JSONObject;

public abstract class RouteAction extends Action {

	static final String ROUTE = "route";
	protected int routeId;
	
	public RouteAction(int routeId) {
		this.routeId = routeId;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(ROUTE, routeId);
		return json;
	}
}
