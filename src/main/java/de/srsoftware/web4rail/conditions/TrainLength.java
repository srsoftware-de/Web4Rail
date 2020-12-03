package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public class TrainLength extends Condition {
	
	private static final String MAX_LENGTH = "max_length";
	private int maxLength = 0;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context.train())) return false;
		return (context.train().length() < maxLength) != inverted;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(MAX_LENGTH, maxLength);
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(MAX_LENGTH)) maxLength = json.getInt(MAX_LENGTH);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Maximum train length"),new Input(MAX_LENGTH, maxLength).numeric());
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements
	}
	
	@Override
	public String toString() {
		return t(inverted ? "train is longer than {}" : "train is shorter than {}",maxLength) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(MAX_LENGTH)) try {
			int ml = Integer.parseInt(params.get(MAX_LENGTH));
			if (ml < 1) throw new NumberFormatException(t("length must be larger than zero!"));
			maxLength = ml;
		} catch (NumberFormatException nfe) {
			Window win = properties();
			win.children().insertElementAt(new Tag("div").content(nfe.getMessage()),1);
			return win;
		}
		return super.update(params);
	}
}
