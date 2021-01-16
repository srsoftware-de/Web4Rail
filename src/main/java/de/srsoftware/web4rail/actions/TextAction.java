package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public abstract class TextAction extends Action {
	
	public TextAction(BaseClass parent) {
		super(parent);
	}

	public static final String TEXT = "text";
	protected String text = "Hello, world!";


	public String fill(String tx,Context context) {
		if (isSet(context.block())) tx = tx.replace("%block%", context.block().name);
		if (isSet(context.contact())) tx = tx.replace("%contact%", context.contact().id().toString());
		if (isSet(context.route())) tx = tx.replace("%route%", context.route().name());
		if (isSet(context.train())) tx = tx.replace("%train%", context.train().name());		
		return tx;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TEXT, text);
		return json;
	}
	
	protected abstract Label label();

	@Override
	public Action load(JSONObject json) {
		super.load(json);
		text = json.getString(TEXT);
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Text"),new Input(TEXT, text));
		return super.properties(preForm, formInputs, postForm);
	}
		
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(TEXT)) text = params.get(TEXT);
		return super.update(params);
	}
}
