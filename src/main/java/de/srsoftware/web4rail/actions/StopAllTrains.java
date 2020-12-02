package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.moving.Train;

public class StopAllTrains extends Action {

	public StopAllTrains(Context parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		Train.list().forEach(train -> train.stopNow());
		return true;
	}
}
