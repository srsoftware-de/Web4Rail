package de.srsoftware.web4rail;

public abstract class DelayedExecution extends Thread {
	private int delay;

	public DelayedExecution(Object cause) {
		this(1000,cause);
	}
	
	public DelayedExecution(int delay,Object cause) {
		this.delay = delay;
		
		setName(Application.threadName(cause));
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
