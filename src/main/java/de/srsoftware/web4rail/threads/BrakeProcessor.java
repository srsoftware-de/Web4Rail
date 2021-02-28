package de.srsoftware.web4rail.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.moving.Train;

public class BrakeProcessor extends Thread {
	public static final Logger LOG = LoggerFactory.getLogger(BrakeProcessor.class);

	
	private long latestTick;
	private static final int SPEED_STEP = 5;
	private Integer timeStep;
	private Route route;
	private Train train;
	private String brakeId;
	private long estimatedDistance; // Unit: s*km/h "km/h-Sekunden"
	private int startSpeed,endSpeed;
	private boolean aborted,modified,finished;
	public static int defaultEndSpeed = 10;
	
	public BrakeProcessor(Route route, Train train) {
		this.train = train;
		this.route = route;
		
		aborted = false;
		modified = false;
		finished = false;
		brakeId = train.brakeId();
		startSpeed = train.speed;		
		endSpeed = defaultEndSpeed;
		
		timeStep = route.brakeTime(brakeId);

		if (BaseClass.isNull(timeStep) || timeStep>1000000) timeStep = 256; 			// if no brake time is available for this train
		setName(Application.threadName("BrakeProcessor("+train+")"));
		start();
	}
	
	public void abort() {
		aborted = true;
	}

	private long calcDistance(Integer ts) {
		long dist = 0;
		int s = startSpeed;
		while (s > defaultEndSpeed) {
			s -= SPEED_STEP;
			dist += s*ts;
		}
		LOG.debug("Estimated distamce with {} ms timestep: {}",ts,dist);
		return dist;
	}
	
	private void checkNextRoute() {
		Route nextRoute = train.nextRoute();
		if (BaseClass.isSet(nextRoute) && nextRoute.state() == Route.State.PREPARED) { // auf Startgeschwindigkeit der Nachfolgeroute bremsen
			Integer nextRouteStartSpeed = nextRoute.startSpeed();
			if (BaseClass.isSet(nextRouteStartSpeed)) {
				LOG.debug("updating target velocity from {} to {}!",endSpeed,nextRouteStartSpeed);
				endSpeed = nextRouteStartSpeed;
				modified = true;
				if (endSpeed > train.speed) train.setSpeed(endSpeed);
			}					
		}
	}
	
	/**
	 * This is called from route.finish when train came to stop
	 */
	public void finish() {
		LOG.debug("BrakeProcessor.finish()");
		finished = true;
		if (aborted || modified) return;
		increaseDistance();
		train.setSpeed(0);
		LOG.debug("Estimated distance: {}",estimatedDistance);

		if (startSpeed <= endSpeed) return;
		if (timeStep<0) timeStep = 100;
		Integer newTimeStep = timeStep;
		long calculated;
		int step = 32*newTimeStep;			
		for (int i=0; i<20; i++) {
			step = step/2;
			if (step<1) step = 1;
			calculated = calcDistance(newTimeStep);
			LOG.debug("Calculated distance for step = {} ms: {}",newTimeStep,calculated);
			LOG.debug("Update step: {}",step);
			newTimeStep = newTimeStep + (calculated > estimatedDistance ? -step : step);				
		}
		
		if (!newTimeStep.equals(timeStep)) {
			route.brakeTime(brakeId,newTimeStep);
			calculated = calcDistance(newTimeStep);
			LOG.debug("Corrected brake timestep for {} @ {} from {} to {} ms.",train,route,timeStep,newTimeStep);
			LOG.debug("Differemce from estimated distance: {} ({}%)",estimatedDistance-calculated,100*(estimatedDistance-calculated)/(float)estimatedDistance);
		}
	}
	
	private void increaseDistance(){
		long tick = BaseClass.timestamp();
		estimatedDistance += train.speed * (3+tick-latestTick);
		latestTick = tick;			
	}

	@Override
	public void run() {
		setName(Application.threadName("BreakeProcessor("+train+")"));
		LOG.debug("started BrakeProcessor ({} → {}) for {} with timestep = {} ms.",train.speed,endSpeed,train,timeStep);
		estimatedDistance = 0;			
		latestTick = BaseClass.timestamp();
		while (train.speed > endSpeed) {
			if (finished || aborted) return;
			increaseDistance();
			LOG.debug("BrakeProcessor({}) setting Speed of {}.",route,train);
			train.setSpeed(Math.max(train.speed - SPEED_STEP,endSpeed));
			if (!modified) checkNextRoute();
			try {
				sleep(timeStep);
			} catch (InterruptedException e) {
				LOG.warn("BrakeProcessor interrupted!", e);
			}				
		}
		
		while (!finished && !aborted && !modified) {
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				LOG.warn("BrakeProcessor interrupted!", e);
			}				
			checkNextRoute();
		}
	}

	public void setEndSpeed(Integer newEndSpeed) {
		if (BaseClass.isNull(newEndSpeed)) return;
		endSpeed = newEndSpeed;		
		modified = true;
	}
}
