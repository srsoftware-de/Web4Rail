package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class FinishRoute extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.finish();
		return true;
	}
}
