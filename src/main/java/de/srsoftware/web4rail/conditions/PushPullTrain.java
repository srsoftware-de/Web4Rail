package de.srsoftware.web4rail.conditions;

import de.srsoftware.web4rail.actions.Action.Context;

public class PushPullTrain extends Condition {
	
	@Override
	public boolean fulfilledBy(Context context) {
		return context.train.pushPull != inverted;
	}

	@Override
	public String toString() {
		return t(inverted ? "train is not a push-pull train":"train is a push-pull train") ;
	}
}
