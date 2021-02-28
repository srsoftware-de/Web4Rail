package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.ControlUnit;

public class SetPower extends Action{
	
	public SetPower(BaseClass parent) {
		super(parent);
	}

	private static final String STATE = "state";
	private POWERCHANGE pc = POWERCHANGE.OFF;
	
	enum POWERCHANGE {
		ON, OFF, TOGGLE;
	}

	@Override
	public boolean fire(Context context,Object cause) {
		ControlUnit cu = plan.controlUnit();
		switch (pc) {
		case ON:
			cu.set(true);	
			break;
		case TOGGLE:
			cu.togglePower();
			break;
		default:
			cu.set(false);
			break;
		}
		
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(STATE, pc);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		pc = POWERCHANGE.valueOf(json.getString(STATE));
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag div = new Tag("div");
		new Radio(STATE, POWERCHANGE.ON, t("On"), pc == POWERCHANGE.ON).addTo(div);
		new Radio(STATE, POWERCHANGE.OFF, t("Off"), pc == POWERCHANGE.OFF).addTo(div);
		new Radio(STATE, POWERCHANGE.TOGGLE, t("Toggle"), pc == POWERCHANGE.TOGGLE).addTo(div);
		formInputs.add(t("Set state to"),div);
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		switch (pc) {
		case ON:
			return t("Switch power on");
		case OFF:
			return t("Switch power off");
		default:
			return t("Toggle power");
		}
	}
	
	@Override
	protected Window update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String newState = params.get(STATE);
		if (isSet(newState)) pc = POWERCHANGE.valueOf(newState);
		return parent().properties();
	}
}
