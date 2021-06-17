package de.srsoftware.web4rail.functions;

import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.moving.Locomotive;

public class HeadLight extends DirectedFunction {
	@Override
	public boolean enabled(Decoder decoder) {
		Locomotive loco = decoder.parent();
		if (isNull(loco) || !loco.isFirst()) return false;
		boolean result = isSet(loco) && loco.isFirst() && super.enabled(decoder);
		return result;
	}
}
