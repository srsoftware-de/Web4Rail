package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class AbortActions extends Action{

	public AbortActions(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		context.invalidate();
		return false;
	}
}
