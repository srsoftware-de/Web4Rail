package de.srsoftware.web4rail.conditions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class TrainSpeed extends Condition {
	
	private static final String SPEED = "speed";
	private int treshold = 0;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context.train())) return false;
		return inverted ? context.train().speed > treshold : context.train().speed < treshold;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(SPEED, treshold);
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(SPEED)) treshold = json.getInt(SPEED);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Train speed"),new Input(SPEED, treshold).numeric().addTo(new Tag("span")).content(speedUnit));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		return t(inverted ? "train is faster than {} {}" : "train is slower than {} {}",treshold,speedUnit) ;
	}

	@Override
	protected Object update(Params params) {
		if (params.containsKey(SPEED)) try {
			int ml = params.getInt(SPEED);
			if (ml < 0) throw new NumberFormatException(t("speed must be non-negative!"));
			treshold = ml;
		} catch (NumberFormatException nfe) {
			Window win = properties();
			win.children().insertElementAt(new Tag("div").content(nfe.getMessage()),1);
			return win;
		}
		return super.update(params);
	}
}
