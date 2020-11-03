package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;

public class FinishRoute extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		Route route = context.route;
		if (route != null) route.finish();
		return true;
	}
}
