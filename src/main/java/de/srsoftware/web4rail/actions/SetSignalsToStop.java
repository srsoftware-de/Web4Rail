package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tiles.Signal;

public class SetSignalsToStop extends Action {

	private Route route;

	public SetSignalsToStop(Route route) {
		this.route = route;
	}

	@Override
	public void fire() throws IOException {
		route.setSignals(Signal.STOP);
	}

}
