package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class FinishRoute extends Action {

	public FinishRoute(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		Route route = context.route();
		Train train = context.train();
		if (isNull(train)) return false;
		if (isSet(route)) route.finish(train);
		return true;
	}
}
