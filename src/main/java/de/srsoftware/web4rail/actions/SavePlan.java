package de.srsoftware.web4rail.actions;

import java.io.IOException;

import de.srsoftware.web4rail.BaseClass;

public class SavePlan extends Action{

	public SavePlan(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		try {
			plan.save();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}
