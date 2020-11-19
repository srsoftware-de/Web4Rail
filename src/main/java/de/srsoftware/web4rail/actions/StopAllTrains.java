package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.moving.Train;

public class StopAllTrains extends Action {

	@Override
	public boolean fire(Context context) {
		Train.list().forEach(train -> train.stopNow());
		return true;
	}
}
