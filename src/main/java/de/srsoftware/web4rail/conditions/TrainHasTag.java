package de.srsoftware.web4rail.conditions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

public class TrainHasTag extends Condition {
	
	private static final String TAG = "tag";
	private String tag = null;
	
	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(tag)) return true;
		Train train = context.train();
		if (isNull(train)) return false;
		if (isNull(train.tags())) return false;
		return train.tags().contains(tag) != inverted;
	}
	
	@Override
	public void inversionOption(FormInput formInputs) {
		Tag radios = new Tag("div");
		new Radio(INVERTED, "off", t("Markieung muss vorhanden sein"), !inverted).addTo(radios);
		new Radio(INVERTED, "on", t("Markieung darf nicht vorhanden sein"), inverted).addTo(radios);
		formInputs.add(t("Condition"), radios);
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Tag"),new Input(TAG, tag == null ? "" : tag));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (tag == null) return "["+t("Click to setup tag")+"]";
		return t(inverted ? "train does not have tag \"{}\"" : "train has tag \"{}\"",tag) ;
	}

	@Override
	protected Object update(Params params) {
		tag = params.getString(TAG);
		return super.update(params);
	}
}
