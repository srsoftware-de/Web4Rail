package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class BrakeStart extends Action {

	public BrakeStart(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.route())) return false;
		context.route().brakeStart();
		LOG.debug("Started brake process...");
		return true;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
