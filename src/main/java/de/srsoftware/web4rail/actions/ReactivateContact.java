package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;

public class ReactivateContact extends Action{

	public ReactivateContact(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(context.contact())) return false;
		if (isNull(context.route())) return false;
		return context.route().reactivate(context.contact());
	}
}
