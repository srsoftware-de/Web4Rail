package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class DelayedAction extends Action {
	
	private static final String ACTIONS = "actions";
	public static final String DELAY = "delay";
	private static final int DEFAULT_DELAY = 1000;
	private int delay = DEFAULT_DELAY;

	private ActionList actions = new ActionList();
	
	private Tag actionsForm(HashMap<String, String> params) {
		Fieldset fieldset = new Fieldset(t("Actions"));
		actions.addTo(fieldset, params.get(CONTEXT));
		return fieldset;
	}
	
	public ActionList children() {
		return actions;
	}
	
	private Tag delayForm(HashMap<String, String> params) {
		Fieldset fieldset = new Fieldset(t("Delay"));

		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);

		new Input(DELAY,delay).numeric().addTo(new Label(t("Delay")+NBSP)).content(" ms").addTo(form);
		return new Button(t("Apply"),form).addTo(form).addTo(fieldset);
	}
		
	@Override
	public boolean fire(Context context) throws IOException {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					LOG.warn("Interrupted Exception thrown while waiting:",e);
				}
				actions.fire(context);
			};
		}.start();
		return true;		
	}


	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(DELAY, delay);
		json.put(ACTIONS, actions.json());
		return json;
	}
	
	public static DelayedAction load(JSONObject json) {
		DelayedAction action = new DelayedAction();
		action.delay = json.getInt(DELAY);
		if (json.has(ACTIONS)) action.actions = ActionList.load(json.getJSONArray(ACTIONS));
		return action;
	}
		
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		delayForm(params).addTo(win);
		actionsForm(params).addTo(win);
		return win;
	}

	@Override
	public String toString() {	
		return t("Wait {} ms, then:",delay);
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String d = params.get(DELAY);
		if (d != null)	try {
			int ms = Integer.parseInt(d);
			if (ms < 0) throw new NumberFormatException(t("Delay must not be less than zero!"));
			delay = ms;
		} catch (NumberFormatException nfe) {
			Window props = properties(params);
			props.children().insertElementAt(new Tag("div").content(nfe.getMessage()), 2);
			return props;
		}		
		return super.update(params);
	}
}
