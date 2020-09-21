package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends RouteAction {


	public SetSignalsToStop(Route route) {
		super(route);
	}

	@Override
	public void fire() throws IOException {
		route.setSignals(Signal.STOP);
	}
}
