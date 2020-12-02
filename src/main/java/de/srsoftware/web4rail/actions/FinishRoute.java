package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Route;

public class FinishRoute extends Action {

	public FinishRoute(Context parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		Route route = context.route();
		if (isSet(route)) route.finish();
		return true;
	}
}
