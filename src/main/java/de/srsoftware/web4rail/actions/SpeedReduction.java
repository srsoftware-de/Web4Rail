package de.srsoftware.web4rail.actions;

import org.json.JSONObject;

import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class SpeedReduction extends RouteAction {

	static final String MAX_SPEED = "max_speed";
	private int maxSpeed;

	public SpeedReduction(int routeId, int kmh) {
		super(routeId);
		maxSpeed = kmh;
	}

	@Override
	public void fire(Route route) {
		Train train = route.train;
		if (train != null && train.speed > maxSpeed) train.setSpeed(maxSpeed);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(MAX_SPEED, maxSpeed);
		return json;
	}
}
