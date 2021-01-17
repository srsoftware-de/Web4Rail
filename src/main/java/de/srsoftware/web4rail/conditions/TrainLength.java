package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class TrainLength extends Condition {
	
	private static final String LENGTH = "length";
	private int treshold = 0;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context.train())) return false;
		int len = context.train().length();
		return inverted ? len > treshold : len < treshold;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(LENGTH, treshold);
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(LENGTH)) treshold = json.getInt(LENGTH);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Maximum train length"),new Input(LENGTH, treshold).numeric().addTo(new Tag("span")).content(lengthUnit));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return t(inverted ? "train is longer than {} {}" : "train is shorter than {} {}",treshold,lengthUnit) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(LENGTH)) try {
			int ml = Integer.parseInt(params.get(LENGTH));
			if (ml < 1) throw new NumberFormatException(t("length must be larger than zero!"));
			treshold = ml;
		} catch (NumberFormatException nfe) {
			Window win = properties();
			win.children().insertElementAt(new Tag("div").content(nfe.getMessage()),1);
			return win;
		}
		return super.update(params);
	}
}
