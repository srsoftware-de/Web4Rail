package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class StopAuto extends Action {

	public StopAuto(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		context.train().quitAutopilot();
		return true;
	}

	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
