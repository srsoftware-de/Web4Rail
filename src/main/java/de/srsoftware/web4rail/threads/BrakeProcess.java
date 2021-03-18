package de.srsoftware.web4rail.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;

public class BrakeProcess extends BaseClass implements Runnable{

	private static final Logger LOG = LoggerFactory.getLogger(BrakeProcess.class);
	
	public static int defaultTimeStep = 500;
	private Train train;
	private int targetSpeed = Train.defaultEndSpeed;
	boolean ended = false;
	long distance = 0;
	private int startSpeed;
	private int lastSpeed;
	private long lastTime;
	
	public BrakeProcess(Train train) {
		this.train = train;		
		new Thread(this, Application.threadName(this)).start();
	}
	
	public BrakeProcess end() {
		LOG.debug("{}.end()",this);
		ended = true;		
		return this;
	}
	
	@Override
	public void run() {
		Integer delay = train.route().brakeTime(train.brakeId());
		startSpeed = train.speed;
		lastTime = timestamp();
		while (!train.hasNextPreparedRoute()) {
			sleep(delay);
			lastSpeed = train.speed;
			updateDistance();
			if (lastSpeed > targetSpeed) lastSpeed -= 10;
			if (ended) break;
			if (lastSpeed <= targetSpeed && (ended = true)) lastSpeed = targetSpeed;
			train.setSpeed(lastSpeed);
		}
	}
	
	private void updateDistance() {
		long newTime = timestamp();
		distance += (newTime-lastTime)*lastSpeed;
		lastTime = newTime;
		LOG.debug("measured distance: {} units",distance);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+train+")";
	}

	public void updateTime() {
		updateDistance();
		LOG.debug("updateTime(): start speed was {} {}.",startSpeed,BaseClass.speedUnit);
		// TODO
	}
}
