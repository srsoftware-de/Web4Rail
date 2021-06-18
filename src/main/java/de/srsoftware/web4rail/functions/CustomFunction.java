package de.srsoftware.web4rail.functions;

import org.json.JSONObject;

import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class CustomFunction extends Function {
	private String customName = super.name();
	
	@Override
	public Fieldset form(Decoder decoder) {
		Fieldset fieldset = super.form(decoder);
		String prefix = "functions/"+id()+"/";
		new Input(prefix+NAME, customName)
			.addTo(new Label(t("Name")))
			.addTo(fieldset);
		return fieldset;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, customName);
		return json;
	}
	
	@Override
	public CustomFunction load(JSONObject json) {
		super.load(json);
		if (json.has(NAME)) customName = json.getString(NAME);
		return this;
	}
	
	@Override
	public String name() {
		return customName;
	}
	
	@Override
	public Object update(Params params) {
		String newName = params.getString(NAME);
		if (isSet(newName) && !newName.isEmpty()) customName = newName;
		return super.update(params);
	}
}
