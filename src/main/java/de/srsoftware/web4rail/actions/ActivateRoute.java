package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class ActivateRoute extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.activate();
		return true;
	}
}
