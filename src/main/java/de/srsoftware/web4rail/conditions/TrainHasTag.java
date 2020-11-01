package de.srsoftware.web4rail.conditions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TrainHasTag extends Condition {
	
	private static final String TAG = "tag";
	private String tag = null;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (tag == null) return true;
		return context.train.tags().contains(tag);
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(TAG, tag);
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(TAG)) tag = json.getString(TAG);
		return this;
	}

	@Override
	public Tag propForm(HashMap<String, String> params) {
		return new Input(TAG, tag == null ? "" : tag).addTo(new Label(t("Tag:")+NBSP)).addTo(super.propForm(params));
	}

	@Override
	public String toString() {
		if (tag == null) return t("[Click to setup tag]");
		return t(inverted ? "train does not have tag \"{}\"" : "train has tag \"{}\"",tag) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		tag = params.get(TAG);
		return super.update(params);
	}
}
