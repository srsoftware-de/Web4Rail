package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends RouteAction {

	public SetSignalsToStop(int routeId) {
		super(routeId);
	}

	@Override
	public void fire(Context context) throws IOException {
		context.route.setSignals(Signal.STOP);
	}
}
