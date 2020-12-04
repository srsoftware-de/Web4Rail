package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class BrakeStop extends Action {

	public BrakeStop(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		if (isNull(context.route())) return false;
		context.route().brakeStop();
		return true;
	}
}
