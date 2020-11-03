package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends Action {

	@Override
	public boolean fire(Context context) {
		context.route.setSignals(Signal.STOP);
		return true;
	}
}
