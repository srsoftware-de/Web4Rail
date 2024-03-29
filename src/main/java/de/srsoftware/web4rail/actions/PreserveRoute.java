package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tiles.Block;

public class PreserveRoute extends Action {

	public PreserveRoute(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (context.invalidated()) return false;
		Train train = context.train();
		Route route = context.route();
		// These are errors:
		if (isNull(train)) return false; 
		if (isNull(route)) return false;
		
		// These are NOT errors:
		if (!train.usesAutopilot()) return true; // do not reserve routes, when not in auto-mode
		Block endBlock = route.endBlock();
		if (route.endsAt(train.destination())) return true; // do not reserve routes, when destination has been reached

		Integer waitTime = context.waitTime();
		if (isSet(waitTime) && waitTime > 0) {
			LOG.debug("Not preserving route, as train needs to stop for {} ms at {}!",waitTime,endBlock);
			return false; // train is expected to wait in next block.
		}
		if (isSet(route.getNextPreparedRoute())) return true;
		Context nextContext = new Context(train).block(route.endBlock()).direction(route.endDirection);
		context.onInvalidate(nextContext::invalidate);
		return route.prepareNext(nextContext);
	}
}
