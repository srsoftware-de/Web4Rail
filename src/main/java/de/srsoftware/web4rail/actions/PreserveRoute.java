package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class PreserveRoute extends Action {

	@Override
	public boolean fire(Context context) {
		Train train = context.train();
		Route route = context.route();
		// These are errors:
		if (isNull(train)) return false; 
		if (isNull(route)) return false;
		
		Range waitTime = route.endBlock().getWaitTime(train,route.endDirection);

		// These are NOT errors:
		if (!train.usesAutopilot()) return true;
		if (waitTime.max > 0) return true; // train is expected to wait in next block.
		if (train.destination() == route.endBlock()) return true;

		train.reserveNext();
		return true;
	}

}
