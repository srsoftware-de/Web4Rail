package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public abstract class TextAction extends Action {
	
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
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		new Input(TEXT, text).addTo(label()).addTo(form);
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String error = null;
		text = params.get(TEXT);
		Window win = properties(params);
		return new Tag("span").content(error).addTo(win);
	}
}
