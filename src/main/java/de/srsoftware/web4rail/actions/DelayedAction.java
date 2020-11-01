package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public class DelayedAction extends Action{

	public static final String DELAY = "delay";
	private static final int DEFAULT_DELAY = 1000;
	private int delay = DEFAULT_DELAY;
	private Action action = null;

	@Override
	public boolean fire(Context context) {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				}
				if (action != null) try {
					action.fire(context);
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(DELAY, delay);
		json.put(ACTION, action.json());
		return json;
	}
	
	public static DelayedAction load(JSONObject json) {
		DelayedAction da = new DelayedAction();
		da.delay = json.getInt(DELAY);
		da.action = Action.load(json.getJSONObject(ACTION));
		return da;
	}
	
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		
		if (action == null) {
			Select select = new Select(TYPE);
			List<Class<? extends Action>> classes = List.of(
					ConditionalAction.class,
					SetSpeed.class,
					SetSignalsToStop.class,
					FinishRoute.class,
					TurnTrain.class,
					StopAuto.class,
					PowerOff.class,
					SetRelay.class,
					DelayedAction.class
					);
			for (Class<? extends Action> clazz : classes) select.addOption(clazz.getSimpleName());
			select.addTo(new Label(t("Action type:")+NBSP)).addTo(form);
		} else {
			action.link(0, params.get(CONTEXT)).addTo(new Label(t("Action: "))).addTo(form);
		}
		Label label = new Label(t("Delay:")+NBSP);
		new Input(DELAY, delay).numeric().addTo(label).content(NBSP+t("miliseconds"));
		label.addTo(form);		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	@Override
	public String toString() {
		if (action == null) return t("Click here to set up delayed action!");
		return t("Wait {} ms, then {}",delay,action);
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		
		String type = params.get(TYPE);
		if (type != null) action = Action.create(type);

		String error = null;
		String ms = params.get(DELAY);
		if (ms == null) {
			ms = ""+DEFAULT_DELAY;
		} else {
			try {
				int d = Integer.parseInt(ms);
				if (d<0) error = t("Delay must not be less than zero!");
				if (error == null) {
					this.delay = d;
					return t("Action updated!");
				}
			} catch (NumberFormatException e) {
				error = t("Not a valid number!");
			}
		}
		Window win = properties(params);
		return new Tag("span").content(error).addTo(win);
	}
}
