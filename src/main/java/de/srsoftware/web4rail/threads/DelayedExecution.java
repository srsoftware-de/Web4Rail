package de.srsoftware.web4rail.threads;

import de.srsoftware.web4rail.Application;

public abstract class DelayedExecution extends Thread {
	private int delay;

	public DelayedExecution(Object cause) {
		this(1000,cause);
	}
	
	public DelayedExecution(int delay,Object cause) {
		super(Application.threadName(cause));
		this.delay = delay;
		start();
	}

	@Override
	public void run() {
		try {
			sleep(delay);
		} catch (InterruptedException e) {}
		execute();
	}
	
	public abstract void execute();
}
