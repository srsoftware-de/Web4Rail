package de.srsoftware.web4rail.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class BrakeProcess extends BaseClass implements Runnable{

	private static final Logger LOG = LoggerFactory.getLogger(BrakeProcess.class);

	private static final int SPEED_STEP = 10;
	
	public static int defaultTimeStep = 500;
	private Train train;
	private int targetSpeed = Train.defaultEndSpeed;
	boolean ended = false;
	long distance = 0;
	private int startSpeed;
	private int lastSpeed;
	private long lastTime;

	private Integer timeStep;

	private Route route;

	private String brakeId;
	
	public BrakeProcess(Train train) {
		this.train = train;
		this.brakeId = train.brakeId();
		this.route = train.route();
		new Thread(this, Application.threadName(this)).start();
	}
	
	private long calcDistance(Integer ts) {
		long dist = 0;
		int s = startSpeed;
		while (s > Train.defaultEndSpeed) {
			s -= SPEED_STEP;
			dist += s*ts;
		}
		LOG.debug("Estimated distamce with {} ms timestep: {}",ts,dist);
		return dist;
	}
	
	public BrakeProcess end() {
		LOG.debug("{}.end()",this);
		ended = true;		
		return this;
	}
	
	@Override
	public void run() {
		timeStep = train.route().brakeTime(train.brakeId());
		if (timeStep == null) timeStep = defaultTimeStep;
		startSpeed = train.speed;
		lastTime = timestamp();
		while (!train.hasNextPreparedRoute()) {
			sleep(timeStep);
			lastSpeed = train.speed;
			updateDistance();
			if (lastSpeed > targetSpeed) lastSpeed -= SPEED_STEP;
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
		Integer newTimeStep = timeStep;
		long calculated;
		int step = 32*newTimeStep;			
		for (int i=0; i<20; i++) {
			step = step/2;
			if (step<1) step = 1;
			calculated = calcDistance(newTimeStep);
			LOG.debug("Calculated distance for step = {} ms: {}",newTimeStep,calculated);
			LOG.debug("Update step: {}",step);
			newTimeStep = newTimeStep + (calculated > distance ? -step : step);				
		}
		
		if (!newTimeStep.equals(timeStep)) {
			route.brakeTime(brakeId,newTimeStep);
			calculated = calcDistance(newTimeStep);
			LOG.debug("Corrected brake timestep for {} @ {} from {} to {} ms.",train,route,timeStep,newTimeStep);
			LOG.debug("Differemce from estimated distance: {} ({}%)",distance-calculated,100*(distance-calculated)/(float)distance);
		}
	}
}
