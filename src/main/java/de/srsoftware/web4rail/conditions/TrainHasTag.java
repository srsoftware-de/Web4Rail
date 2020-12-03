package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public class TrainHasTag extends Condition {
	
	private static final String TAG = "tag";
	private String tag = null;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (tag == null) return true;
		return context.train().tags().contains(tag) != inverted;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Tag"),new Input(TAG, tag == null ? "" : tag));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements
	}
	
	@Override
	public String toString() {
		if (tag == null) return "["+t("Click to setup tag")+"]";
		return t(inverted ? "train does not have tag \"{}\"" : "train has tag \"{}\"",tag) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		tag = params.get(TAG);
		return super.update(params);
	}
}
