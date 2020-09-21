package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class FinishRoute extends RouteAction {

	public FinishRoute(int routeId) {
		super(routeId);
	}

	@Override
	public void fire(Route route) throws IOException {
		route.finish();
	}
}
