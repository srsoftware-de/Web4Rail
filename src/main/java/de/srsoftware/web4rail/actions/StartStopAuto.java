package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

public class StartStopAuto extends Action {

	private static final String INVERTED = "inverted";
	public boolean inverted = false;
	
	public StartStopAuto(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(context.train())) return false;
		if (inverted) {
			context.train().start(true);
		} else context.train().quitAutopilot();
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Tag radios = new Tag("div");
		new Radio(INVERTED, "on", t("Start autopilot"), inverted).addTo(radios);
		new Radio(INVERTED, "off", t("Stop autopilot"), !inverted).addTo(radios);
		formInputs.add(t("Action"), radios);
		
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		return inverted ? t("Start autopilot") : t("Stop autopilot");
	}
	
	protected Object update(Params params) {
		inverted = "on".equals(params.get(INVERTED));
		return super.update(params);
	}
}
