package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class SetSpeed extends Action{

	public SetSpeed(BaseClass parent) {
		super(parent);
	}

	public static final String MAX_SPEED = "max_speed";
	private int speed = 0;

	@Override
	public boolean correspondsTo(Action other) {
		return other instanceof SetSpeed;
	}
	
	@Override
	public boolean fire(Context context) {
		if (context.invalidated()) return false;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Set speed to"),new Input(MAX_SPEED, speed).numeric().addTo(new Tag("span")).content(NBSP+speedUnit));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	public int getSpeed() {
		return speed;
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
	protected Object update(Params params) {
		String error = null;
		Integer ms = params.getInt(MAX_SPEED);
		if (ms == null) ms = 128;
		if (ms<0) error = t("Speed must not be less than zero!");
		if (isSet(error)) return error;
		speed = ms;
		return super.update(params);
	}
}
