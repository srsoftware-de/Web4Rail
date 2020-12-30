package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;

public class AddRemoveTag extends Action{

	private static final String TAG = "tag";

	public AddRemoveTag(BaseClass parent) {
		super(parent);
	}

	private String tag = "test";
	private boolean add = true;
	
	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		if (add) {
			context.train().tags().add(tag);
		} else {
			context.train().tags().remove(tag);
		}
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TAG, tag);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		tag = json.getString(TAG);
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Tag"),new Input(TAG, tag));
		Tag div = new Tag("div");
		new Radio(TYPE, ACTION_ADD, t("add"), add).addTo(div);
		new Radio(TYPE, ACTION_DROP, t("delete"), !add).addTo(div);
		formInputs.add(t("Action"),div);
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return add ? t("Add tag \"{}\" to train",tag) : t("Remove tag \"{}\" from train",tag);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		tag = params.get(TAG);
		add = ACTION_ADD.equals(params.get(TYPE));
		return super.update(params);
	}
}
