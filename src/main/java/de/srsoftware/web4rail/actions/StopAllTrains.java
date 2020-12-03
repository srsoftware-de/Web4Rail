package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;

public class StopAllTrains extends Action {

	public StopAllTrains(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		Train.list().forEach(train -> train.stopNow());
		return true;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
