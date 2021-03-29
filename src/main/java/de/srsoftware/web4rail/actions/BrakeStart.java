package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class BrakeStart extends Action {

	public BrakeStart(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(context.train())) return false;
		context.train().startBrake();
		return true;
	}
}
