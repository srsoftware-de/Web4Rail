package de.srsoftware.web4rail.threads;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

public class Simulator extends BaseClass implements Runnable {

	private Route route;

	public Simulator(Route route) {
		this.route = route;
		new Thread(this,Application.threadName(this)).start();
	}

	@Override
	public void run() {
		for (Tile tile : route.path()) {
			sleep(1000);
			if (tile instanceof Contact) ((Contact)tile).activate(true);
		}
	}

}
