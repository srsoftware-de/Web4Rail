package de.srsoftware.web4rail.conditions;

public class TrainIsShunting extends Condition {
	
	@Override
	public boolean fulfilledBy(Context context) {
		return context.train().isShunting() != inverted;
	}

	@Override
	public String toString() {
		return t(inverted ? "train is not shunting":"train is shunting") ;
	}
}
