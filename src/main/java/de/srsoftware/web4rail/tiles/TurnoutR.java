package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.ControlUnit.Reply;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TurnoutR extends Turnout {
	
	private static final String RIGHT = "right";

	@Override
	public Object click() throws IOException {
		Object o = super.click();
		if (route != null) {
			plan.stream(t("{} is locked by {}!",this,route)); 
		} else state(state == State.STRAIGHT ? State.RIGHT : State.STRAIGHT);
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
		new Input(RIGHT, portB).numeric().addTo(new Label(t("Right port"))).addTo(fieldset);
		return form;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(STRAIGHT)) portA = Integer.parseInt(params.get(STRAIGHT));
		if (params.containsKey(RIGHT)) portB = Integer.parseInt(params.get(RIGHT));
		return super.update(params);
	}
	
	@Override
	public CompletableFuture<Reply> state(State newState) throws IOException {
		init();
		LOG.debug("Setting {} to {}",this,newState);
		CompletableFuture<Reply> result;
		switch (newState) {
		case RIGHT:
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
}
