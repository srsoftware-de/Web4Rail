package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends Action {

	@Override
	public boolean fire(Context context) throws IOException {
		context.route.setSignals(Signal.STOP);
		return true;
	}
}
