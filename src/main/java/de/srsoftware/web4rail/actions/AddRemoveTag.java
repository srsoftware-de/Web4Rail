package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

public class AddRemoveTag extends Action{

	private static final String TAG = "tag";

	public AddRemoveTag(BaseClass parent) {
		super(parent);
	}

	private String tag = "test";
	private boolean remove = false;
	
	@Override
	public boolean fire(Context context) {
		if (isNull(context.train())) return false;
		if (remove) {
			context.train().removeTag(tag);			
		} else {
			context.train().addTag(tag);			
		}
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TAG, tag);
		if (remove) json.put(ACTION_DROP, true);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(TAG)) tag = json.getString(TAG);
		if (json.has(ACTION_DROP)) remove = json.getBoolean(ACTION_DROP);
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Tag"),new Input(TAG, tag));
		Tag div = new Tag("div");
		new Radio(TYPE, ACTION_ADD, t("add"), !remove).addTo(div);
		new Radio(TYPE, ACTION_DROP, t("delete"), remove).addTo(div);
		formInputs.add(t("Action"),div);
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return remove ? t("Remove tag \"{}\" from train",tag) : t("Add tag \"{}\" to train",tag);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		tag = params.get(TAG);
		remove = ACTION_DROP.equals(params.get(TYPE));
		return super.update(params);
	}
}
