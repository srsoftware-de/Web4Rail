package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class BrakeCancel extends Action {

	public BrakeCancel(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.route())) return false;
		context.route().brakeCancel();
		return true;
	}
}
