package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class SpeedReduction extends Action {

	private int maxSpeed;
	private Route route;

	public SpeedReduction(Route route, int kmh) {
		this.route = route;
		maxSpeed = kmh;
	}

	@Override
	public void fire() {
		Train train = route.train;
		if (train != null && train.speed > maxSpeed) train.setSpeed(maxSpeed);
	}
}
