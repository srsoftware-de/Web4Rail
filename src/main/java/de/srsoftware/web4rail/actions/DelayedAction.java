package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public class DelayedAction extends ActionList {
	
	public static final String DELAY = "delay";
	private static final int DEFAULT_DELAY = 1000;
	private int delay = DEFAULT_DELAY;
		
	public DelayedAction(BaseClass parent) {
		super(parent);
	}
		
	public boolean equals(DelayedAction other) {
		return (delay+":"+actions).equals(other.delay+":"+other.actions);
	}
		
	@Override
	public boolean fire(Context context) {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(delay);
					LOG.debug("{} ms passed by, firing actions:",delay);
				} catch (InterruptedException e) {
					LOG.warn("Interrupted Exception thrown while waiting:",e);
				}
				DelayedAction.super.fire(context);
			};
		}.start();
		return true;		
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(DELAY, delay);
	}
	
	public DelayedAction load(JSONObject json) {
		super.load(json);
		delay = json.getInt(DELAY);
		return this;
	}
		
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Delay"),new Input(DELAY,delay).numeric().addTo(new Tag("span")).content(NBSP+"ms"));
		return super.properties(preForm, formInputs, postForm);
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
			Window props = properties();
			props.children().insertElementAt(new Tag("div").content(nfe.getMessage()), 2);
			return props;
		}		
		return super.update(params);
	}
}
