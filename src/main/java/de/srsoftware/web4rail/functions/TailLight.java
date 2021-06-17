package de.srsoftware.web4rail.functions;

import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.moving.Locomotive;

public class TailLight extends DirectedFunction {
	@Override
	public boolean enabled(Decoder decoder) {
		Locomotive loco = decoder.parent();
		if (isNull(loco) || !loco.isLast()) return false;
		return super.enabled(decoder);
	}
}
