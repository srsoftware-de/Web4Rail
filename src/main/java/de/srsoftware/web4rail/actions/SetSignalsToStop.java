package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends RouteAction {


	public SetSignalsToStop(int routeId) {
		super(routeId);
	}

	@Override
	public void fire(Route route) throws IOException {
		route.setSignals(Signal.STOP);
	}
}
