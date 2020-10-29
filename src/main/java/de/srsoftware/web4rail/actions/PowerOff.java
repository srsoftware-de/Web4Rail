package de.srsoftware.web4rail.actions;

public class PowerOff extends Action{

	@Override
	public boolean fire(Context context) {
		context.contact.plan().controlUnit().emergency();
		return false;
	}
}
