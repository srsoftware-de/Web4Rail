package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class FinishRoute extends RouteAction {

	public FinishRoute(Route route) {
		super(route);
	}

	@Override
	public void fire() throws IOException {
		route.finish();
	}
}
