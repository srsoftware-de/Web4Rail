package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class ActivateRoute extends RouteAction {

	public ActivateRoute(int routeId) {
		super(routeId);
	}

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.activate();
		return true;
	}
}
