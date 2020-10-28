package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class TurnTrain extends RouteAction implements Constants{

	public TurnTrain(int routeId) {
		super(routeId);
	}

	@Override
	public void fire(Route route) {
		Train train = route.train;
		if (train != null) train.turn();
	}
}
