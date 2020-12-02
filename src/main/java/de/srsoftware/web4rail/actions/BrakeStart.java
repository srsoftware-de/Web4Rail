package de.srsoftware.web4rail.actions;

public class BrakeStart extends Action {

	public BrakeStart(Context parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.route())) return false;
		context.route().brakeStart();
		LOG.debug("Started brake process...");
		return true;
	}

}
