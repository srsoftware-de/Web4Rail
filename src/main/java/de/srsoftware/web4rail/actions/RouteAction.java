package de.srsoftware.web4rail.actions;

import java.io.IOException;

import org.json.JSONObject;

import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Route;

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
	
	@Override
	public void fire(Plan plan) throws IOException {
		fire(plan.route(routeId));
	}

	protected abstract void fire(Route route) throws IOException;
}
