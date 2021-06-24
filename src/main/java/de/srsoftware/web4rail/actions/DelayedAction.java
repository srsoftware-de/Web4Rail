package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.DelayedExecution;

public class DelayedAction extends ActionList {
	
	public static final String DELAY = "delay";
	public static final String MIN_DELAY = "min_delay";
	public static final String MAX_DELAY = "max_delay";
	private static final int DEFAULT_DELAY = 1000;
	private int min_delay = DEFAULT_DELAY;
	private int max_delay = DEFAULT_DELAY;
		
	public DelayedAction(BaseClass parent) {
		super(parent);
	}
			
	public boolean equals(DelayedAction other) {
		return (min_delay+":"+max_delay+":"+actions).equals(other.min_delay+":"+other.max_delay+":"+other.actions);
	}
		
	@Override
	public boolean fire(Context context) {
		int delay = min_delay + (min_delay < max_delay ? random.nextInt(max_delay - min_delay) : 0);
		
		new DelayedExecution(delay,this) {
			
			@Override	
			public void execute() {
				LOG.debug("{} ms passed by, firing actions:",delay);
				if (context.invalidated()) return;
				DelayedAction.super.fire(context);
				plan.alter();
			}
		};
		return true;		
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(MIN_DELAY, min_delay).put(MAX_DELAY, max_delay);
	}
	
	public DelayedAction load(JSONObject json) {
		super.load(json);
		if (json.has(DELAY)) {
			min_delay = json.getInt(DELAY);
			max_delay = json.getInt(DELAY);
		}		
		if (json.has(MIN_DELAY)) min_delay = json.getInt(MIN_DELAY);
		if (json.has(MAX_DELAY)) max_delay = json.getInt(MAX_DELAY);
		return this;
	}
		
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Minimum delay"),new Input(MIN_DELAY,min_delay).numeric().addTo(new Tag("span")).content(NBSP+"ms"));
		formInputs.add(t("Maximum delay"),new Input(MAX_DELAY,max_delay).numeric().addTo(new Tag("span")).content(NBSP+"ms"));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	public DelayedAction setMaxDelay(int max_delay) {
		this.max_delay = max_delay;
		return this;
	}

	public DelayedAction setMinDelay(int min_delay) {
		this.min_delay = min_delay;
		return this;
	}

	@Override
	public String toString() {
		return t("Wait {} ms, then",min_delay < max_delay ? min_delay+"…"+max_delay : min_delay)+COL;
	}

	@Override
	protected Object update(Params params) {
		String d = params.getString(MIN_DELAY);
		if (isSet(d))	try {
			int ms = Integer.parseInt(d);
			if (ms < 0) throw new NumberFormatException(t("Delay must not be less than zero!"));
			min_delay = ms;
		} catch (NumberFormatException nfe) {
			Window props = properties();
			props.children().insertElementAt(new Tag("div").content(nfe.getMessage()), 2);
			return props;
		}		
		d = params.getString(MAX_DELAY);
		if (isSet(d))	try {
			int ms = Integer.parseInt(d);
			if (ms < 0) throw new NumberFormatException(t("Delay must not be less than zero!"));
			max_delay = ms;
		} catch (NumberFormatException nfe) {
			Window props = properties();
			props.children().insertElementAt(new Tag("div").content(nfe.getMessage()), 2);
			return props;
		}		
		if (min_delay > max_delay) {
			int dummy = min_delay;
			min_delay = max_delay;
			max_delay = dummy;
		}
		return super.update(params);
	}
}
