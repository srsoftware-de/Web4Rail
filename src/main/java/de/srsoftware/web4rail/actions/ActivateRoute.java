package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class ActivateRoute extends RouteAction {


	public ActivateRoute(Route route) {
		super(route);
	}

	@Override
	public void fire() throws IOException {
		route.activate();
	}
}
