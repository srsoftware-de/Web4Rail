package de.srsoftware.web4rail.actions;

public class StopAuto extends Action {

	public StopAuto(Context parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		context.train().quitAutopilot();
		return true;
	}

}
