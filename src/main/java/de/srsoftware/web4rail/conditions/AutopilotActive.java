package de.srsoftware.web4rail.conditions;

public class AutopilotActive extends Condition {
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context.train())) return false;
		return context.train().usesAutopilot() != inverted;
	}

	@Override
	public String toString() {
		return t(inverted ? "autopilot inactive for train":"autopilot active for train") ;
	}
}
