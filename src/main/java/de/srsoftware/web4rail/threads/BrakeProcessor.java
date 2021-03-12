package de.srsoftware.web4rail.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

/**
 * @author Stephan Richter, SRSoftware
 *
 */
public class BrakeProcessor extends BaseClass implements Runnable {

	private enum State {
		IDLE, BRAKING, ABORTED, ENDED;
	}

	private static final Logger LOG = LoggerFactory.getLogger(BrakeProcessor.class);
	public static int defaultEndSpeed;
	private Train train;
	private State state = State.IDLE;
	private long measuredDistance;
	private long lastTime;
	private Integer brakeTime;
	private int startSpeed;

	public BrakeProcessor(Train train) {
		this.train = train;
	}

	public void end() {
		state = State.ENDED;
		measuredDistance += train.speed * (BaseClass.timestamp() - lastTime);
		Route route = train.route();
		if (isNull(route)) return;
		LOG.debug("old brake time: {}, measured distance: {}", brakeTime, measuredDistance);
		int step = brakeTime;
		for (int i = 0; i < 15; i++) {
			long calculatedDistance = calculate(brakeTime, startSpeed);
			step /= 2;
			if (step < 1) step = 1;
			if (measuredDistance > calculatedDistance) brakeTime += step;
			if (measuredDistance < calculatedDistance) brakeTime -= step;
			LOG.debug("new brake time: {}, calculated distance: {}", brakeTime, calculatedDistance);
		}
		route.brakeTime(train.brakeId(), brakeTime);
	}

	private static long calculate(int brakeTime, int speed) {
		long dist = 0;
		while (speed > defaultEndSpeed) {
			dist += speed * brakeTime;
			speed -= 10;
		}
		return dist;
	}

	@Override
	public void run() {
		LOG.debug("run()");
		Route route = train.route();
		if (isNull(route)) return;
		brakeTime = route.brakeTime(train.brakeId());
		if (isNull(brakeTime)) brakeTime = 250;

		state = State.BRAKING;
		measuredDistance = 0;
		lastTime = BaseClass.timestamp();
		startSpeed = train.speed;
		int targetSpeed = defaultEndSpeed;
		while (state == State.BRAKING) {
			sleep(brakeTime);
			long newTime = BaseClass.timestamp();
			if (isNull(train.route())) state = State.ABORTED;
			if (state != State.BRAKING) break;
			measuredDistance += train.speed * (newTime - lastTime);
			int newSpeed = train.speed - 10;
			if (newSpeed < targetSpeed) {
				train.setSpeed(targetSpeed);
				break;
			}
			train.setSpeed(newSpeed);
			lastTime = newTime;
		}
		LOG.debug("{} reached final speed.", train);
	}

	public BrakeProcessor start() {
		Thread thread = new Thread(this);
		thread.setName(getClass().getSimpleName());
		thread.start();
		return this;
	}

}
