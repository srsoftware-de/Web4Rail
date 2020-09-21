package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class ActivateRoute extends RouteAction {


	public ActivateRoute(int routeId) {
		super(routeId);
	}

	@Override
	public void fire(Route route) throws IOException {
		route.activate();
	}
}
