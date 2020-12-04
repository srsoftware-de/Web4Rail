package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class TurnTrain extends Action{

	public TurnTrain(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (context.train() != null) {
			context.train().turn();
			return true;
		}
		return false;
	}
}
