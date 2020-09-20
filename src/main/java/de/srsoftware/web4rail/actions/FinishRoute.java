package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class FinishRoute extends Action {

	private Route route;

	public FinishRoute(Route route) {
		this.route = route;
	}

	@Override
	public void fire() throws IOException {
		route.finish();
	}

}
