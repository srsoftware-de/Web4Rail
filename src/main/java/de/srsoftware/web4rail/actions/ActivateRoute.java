package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Route;

public class ActivateRoute extends Action {

	private Route route;

	public ActivateRoute(Route route) {
		this.route = route;
	}

	@Override
	public void fire() {
		route.activate();
	}
}
