package de.srsoftware.web4rail.actions;

import java.io.IOException;

public class FreeStartBlock extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.freeStartBlock();
		return true;
	}
}
