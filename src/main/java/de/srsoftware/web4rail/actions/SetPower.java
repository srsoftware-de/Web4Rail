package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.ControlUnit;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;

public class SetPower extends Action{
	
	private static final String STATE = "state";
	private POWERCHANGE pc = POWERCHANGE.OFF;
	
	enum POWERCHANGE {
		ON, OFF, TOGGLE;
	}

	@Override
	public boolean fire(Context context) {
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
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);

		new Radio(STATE, POWERCHANGE.ON, t("On"), pc == POWERCHANGE.ON).addTo(form);
		new Radio(STATE, POWERCHANGE.OFF, t("Off"), pc == POWERCHANGE.OFF).addTo(form);
		new Radio(STATE, POWERCHANGE.TOGGLE, t("Toggle"), pc == POWERCHANGE.TOGGLE).addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
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
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String error = null;
		String newState = params.get(STATE);
		if (isSet(newState)) pc = POWERCHANGE.valueOf(newState);
		Window win = properties(params);
		return new Tag("span").content(error).addTo(win);
	}
}
