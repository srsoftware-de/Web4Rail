package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;

public class StopAllTrains extends Action {

	public StopAllTrains(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		BaseClass.listElements(Train.class).forEach(train -> train.stopNow());
		return true;
	}
}
