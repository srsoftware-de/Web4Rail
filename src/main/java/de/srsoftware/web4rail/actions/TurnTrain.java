package de.srsoftware.web4rail.actions;

public class TurnTrain extends Action{

	public TurnTrain(Context parent) {
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
