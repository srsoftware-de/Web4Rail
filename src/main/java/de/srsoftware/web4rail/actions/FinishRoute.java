package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class FinishRoute extends RouteAction {

	public FinishRoute(int routeId) {
		super(routeId);
	}

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.finish();
		return true;
	}
}
