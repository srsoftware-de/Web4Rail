package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public class SetSpeed extends Action{

	public SetSpeed(BaseClass parent) {
		super(parent);
	}

	public static final String MAX_SPEED = "max_speed";
	private int speed = 0;

	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		context.train().setSpeed(speed);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(MAX_SPEED, speed);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		speed = json.getInt(MAX_SPEED);
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Set speed to"),new Input(MAX_SPEED, speed).numeric());
		return super.properties(preForm, formInputs, postForm);
	}
		
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
	
	@Override
	public String toString() {
		return t("Set speed to {} {}",speed,speedUnit);
	}
	
	public SetSpeed to(int newSpeed) {
		speed = newSpeed;
		return this;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String error = null;
		String ms = params.get(MAX_SPEED);
		if (ms == null) {
			ms = ""+128;
		} else {
			try {
				int s = Integer.parseInt(ms);
				if (s<0) error = t("Speed must not be less than zero!");
				if (error == null) {
					this.speed = s;
					return t("Action updated!");
				}
			} catch (NumberFormatException e) {
				error = t("Not a valid number!");
			}
		}
		if (isSet(error)) return error;
		return super.update(params);
	}
}
