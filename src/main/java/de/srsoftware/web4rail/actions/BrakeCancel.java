package de.srsoftware.web4rail.actions;

public class BrakeCancel extends Action {

	@Override
	public boolean fire(Context context) {
		if (isNull(context.route())) return false;
		context.route().brakeCancel();
		return true;
	}

}
