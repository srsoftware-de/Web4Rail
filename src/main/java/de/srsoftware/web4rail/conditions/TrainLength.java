package de.srsoftware.web4rail.conditions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TrainLength extends Condition {
	
	private static final String MAX_LENGTH = "max_length";
	private int maxLength = 0;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context.train)) return false;
		return (context.train.length() < maxLength) != inverted;
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
	public Tag propForm(HashMap<String, String> params) {
		return new Input(MAX_LENGTH, maxLength).numeric().addTo(new Label(t("Maximum train length:")+NBSP)).addTo(super.propForm(params));
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
			Window win = properties(params);
			win.children().insertElementAt(new Tag("div").content(nfe.getMessage()),1);
			return win;
		}
		return super.update(params);
	}
}
