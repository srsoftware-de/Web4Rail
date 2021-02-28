package de.srsoftware.web4rail.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Range;
import de.srsoftware.web4rail.moving.Train;

public class Autopilot extends Thread{
	
	private static final Logger LOG = LoggerFactory.getLogger(Autopilot.class);
	
	boolean stop = false;
	private Train train;
	int waitTime = 100;
	
	public Autopilot(Train train) {
		this.train = train;
		setName(Application.threadName("Autopilot("+train+")"));
		start();
	}
	
	public void doStop() {
		stop = true;
	}
	
	@Override
	public void run() {
		try {
			stop = false;
			while (true) {
				if (BaseClass.isNull(train.route())) {
					Thread.sleep(waitTime);
					if (waitTime > 100) waitTime /=2;
					if (stop) break;
					if (BaseClass.isNull(train.route())) { // may have been set by start action in between
						String message = train.start();
						if (BaseClass.isSet(train.route())) {
							LOG.debug("{}.start called, route now is {}",train,train.route());
							BaseClass.plan.stream(message);
							//if (isSet(destination)) Thread.sleep(1000); // limit load on PathFinder
						} else {
							LOG.debug(message);
							waitTime = 1000; // limit load on PathFinder
						}
					}						
				} else {
					if (stop) break;
					Thread.sleep(250);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		train.autopilot = null;
		if (BaseClass.isSet(train.currentBlock())) BaseClass.plan.place(train.currentBlock());
	}

	public void waitTime(Range wt) {
		this.waitTime = wt.random();
		String msg = BaseClass.t("{} waiting {} secs...",this,waitTime/1000d);
		LOG.debug(msg);
		BaseClass.plan.stream(msg);

	}
}
