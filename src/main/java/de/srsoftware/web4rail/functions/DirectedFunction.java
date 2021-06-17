package de.srsoftware.web4rail.functions;

import org.json.JSONObject;

import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;

public class DirectedFunction extends Function {
	
	private boolean forward,reverse;
	
	@Override
	public boolean enabled(Decoder decoder) {
		if (!super.enabled(decoder)) return false;
		if (decoder.reverse()) {
			return reverse;
		} else return forward;
	}
	
	@Override
	public Fieldset form(Decoder decoder) {
		Fieldset fieldset = super.form(decoder);
		String prefix = "functions/"+id()+"/";
		new Checkbox(prefix+FORWARD, t(FORWARD), forward, true).addTo(fieldset);
		new Checkbox(prefix+REVERSE, t(REVERSE), reverse, true).addTo(fieldset);
		return fieldset;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (forward) json.put(FORWARD, true);
		if (reverse) json.put(REVERSE, true);
		return json; 
	}
	
	@Override
	public DirectedFunction load(JSONObject json) {
		super.load(json);
		if (json.has(FORWARD)) forward = true;
		if (json.has(REVERSE)) reverse = true;
		return this;
	}
	
	@Override
	public Object update(Params params) {
		if (params.containsKey(FORWARD)) forward = "on".equals(params.get(FORWARD));
		if (params.containsKey(REVERSE)) reverse = "on".equals(params.get(REVERSE));
		return super.update(params);
		
	}
}
