package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.ControlUnit.Reply;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TurnoutL extends Turnout {

	private static final String LEFT = "left";

	@Override
	public Object click() throws IOException {
		Object o = super.click();
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else {
			CompletableFuture<Reply> promise = state(state == State.STRAIGHT ? State.LEFT : State.STRAIGHT);
			promise.exceptionally(ex -> {
				LOG.warn("Failed to toggle turnout: ",ex);
				throw new RuntimeException(ex);
			}).thenAccept(reply -> LOG.debug("Success: {}",reply));
		}
		return o;
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		Tag fieldset = null;
		for (Tag child : form.children()) {
			if (child.is(Fieldset.TYPE)) {
				fieldset = child;
				break;
			}
		}
		new Input(STRAIGHT, portA).numeric().addTo(new Label(t("Straight port"))).addTo(fieldset);
		new Input(LEFT, portB).numeric().addTo(new Label(t("Left port"))).addTo(fieldset);
		return form;
	}
	
	@Override
	public CompletableFuture<Reply> state(State newState) throws IOException {
		init();
		LOG.debug("Requesting to set {} to {}",this,newState);
		CompletableFuture<Reply> result;
		switch (newState) {
		case LEFT:
			result = plan.queue("SET {} GA "+address+" "+portB+" 1 "+delay);
			break;
		case STRAIGHT:
			result = plan.queue("SET {} GA "+address+" "+portA+" 1 "+delay);
			break;
		default:
			throw new IllegalStateException();
		}
		return result.thenApply(reply -> {
			LOG.debug("{} received {}",getClass().getSimpleName(),reply);
			if (!reply.is(200)) error(reply);
			state = newState;
			success();
			return reply;
		});
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(LEFT)) portB = Integer.parseInt(params.get(LEFT));
		return super.update(params);
	}
}
