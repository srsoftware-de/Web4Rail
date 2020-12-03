package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;

public class FinishRoute extends Action {

	public FinishRoute(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		Route route = context.route();
		if (isSet(route)) route.finish();
		return true;
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
}
