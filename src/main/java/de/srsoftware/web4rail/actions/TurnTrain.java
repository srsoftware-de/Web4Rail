package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Constants;

public class TurnTrain extends RouteAction implements Constants{

	public TurnTrain(int routeId) {
		super(routeId);
	}

	@Override
	public boolean fire(Context context) {
		if (context.train != null) {
			context.train.turn();
			return true;
		}
		return false;
	}
}
