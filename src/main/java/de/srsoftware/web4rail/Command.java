package de.srsoftware.web4rail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Command extends CompletableFuture<ControlUnit.Reply> {

	private static final Logger LOG = LoggerFactory.getLogger(Command.class);
	private String command;
	
	public Command(String command) {
		this.command = command;
		this.orTimeout(500, TimeUnit.MILLISECONDS);
		LOG.debug("Created new Command({}).",command);
	}

	@Override
	public String toString() {
		return command;
	}
}
