package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;

public class StartStopAuto extends Action {

	private static final String INVERTED = "inverted";
	public boolean inverted = false;
	
	public StartStopAuto(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		context.train().quitAutopilot();
		return true;
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject().put(TYPE, getClass().getSimpleName());
		if (inverted) json.put(INVERTED, true);
		return json;
	}
	
	public StartStopAuto load(JSONObject json) {
		inverted = json.has(INVERTED) && json.getBoolean(INVERTED);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("inverted"),new Checkbox(INVERTED, t("inverted"), inverted));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return inverted ? t("Start autopilot") : t("Stop autopilot");
	}
	
	protected Object update(HashMap<String, String> params) {
		inverted = "on".equals(params.get(INVERTED));
		return super.update(params);
	}
}
