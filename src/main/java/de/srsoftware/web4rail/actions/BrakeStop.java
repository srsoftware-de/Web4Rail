package de.srsoftware.web4rail.actions;

public class BrakeStop extends Action {

	@Override
	public boolean fire(Context context) {
		if (isNull(context.route)) return false;
		context.route.brakeStop();
		return true;
	}

}
