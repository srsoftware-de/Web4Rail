package de.srsoftware.web4rail.conditions;

import de.srsoftware.web4rail.BaseClass;

public class PushPullTrain extends Condition {
	
	@Override
	public boolean fulfilledBy(Context context) {
		return context.train().pushPull != inverted;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements
	}

	@Override
	public String toString() {
		return t(inverted ? "train is not a push-pull train":"train is a push-pull train") ;
	}
}
