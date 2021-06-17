package de.srsoftware.web4rail.functions;

import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;

public class HeadLight extends Function {
	
	private boolean forward,reverse;

	public HeadLight() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public Fieldset form(Decoder decoder) {
		Fieldset fieldset = super.form(decoder);
		String prefix = "functions/"+id()+"/";
		new Checkbox(prefix+FORWARD, t(FORWARD), forward).addTo(fieldset);
		new Checkbox(prefix+REVERSE, t(REVERSE), reverse).addTo(fieldset);
		return fieldset;
	}
	
	@Override
	public Object update(Params params) {
		if (params.containsKey(FORWARD)) forward = "on".equals(params.get(FORWARD));
		if (params.containsKey(REVERSE)) reverse = "on".equals(params.get(REVERSE));
		return super.update(params);
		
	}
}
