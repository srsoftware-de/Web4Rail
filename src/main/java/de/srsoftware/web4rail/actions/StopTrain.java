package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class StopTrain extends Action{

	public StopTrain(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(context.train())) return false;
		LOG.debug("{}.fire() called, context = {}",this,context);
		context.train().stopNow();
		return true;
	}
	
	@Override
	public String toString() {
		return t("Stop train immediately");
	}
}
