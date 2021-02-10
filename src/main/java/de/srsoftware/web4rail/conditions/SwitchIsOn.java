package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Switch;

public class SwitchIsOn extends Condition {
	
	private Switch swtch = null;
	private static final String SWITCH = "switch";
	
	

	@Override
	public boolean fulfilledBy(Context context) {
		if (isSet(swtch)) return swtch.isOn() != inverted;
		return false;
	}

	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(swtch)) json.put(SWITCH, swtch.id().toString());		
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(SWITCH)) swtch = BaseClass.get(new Id(json.getString(SWITCH)));
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select switch"),Switch.selector(swtch));
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		if (isNull(SWITCH))  return "["+t("Click here to select switch!")+"]";
		return t(inverted ? "{} is off" : "{} is on",swtch) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String switchId = params.get(Switch.class.getSimpleName());
		if (isSet(switchId)) swtch = BaseClass.get(new Id(switchId));
		return super.update(params);
	}
}
