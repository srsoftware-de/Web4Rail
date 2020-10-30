package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class SetSpeed extends Action{

	public static final String MAX_SPEED = "max_speed";
	private int maxSpeed = -1;

	public SetSpeed(int kmh) {
		super();
		maxSpeed = kmh;
	}

	@Override
	public boolean fire(Context context) {
		if (context.train != null && context.train.speed > maxSpeed) {
			context.train.setSpeed(maxSpeed);
			return true;
		}
		return false;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(MAX_SPEED, maxSpeed);
		return json;
	}
	
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		String formId = "action-prop-form-"+id;
		Form form = new Form(formId);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		Label label = new Label(t("Set speed to")+NBSP);
		new Input(MAX_SPEED, maxSpeed).addTo(label).content(NBSP+t("km/h"));
		label.addTo(form);
		new Button(t("Save"),"return submitForm('"+formId+"');").addTo(form).addTo(win);		
		return win;
	}
	
	@Override
	public String toString() {
		return t("Reduce speed to {} km/h",maxSpeed);
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String error = null;
		String ms = params.get(MAX_SPEED);
		if (ms == null) {
			ms = ""+128;
		} else {
			try {
				int s = Integer.parseInt(ms);
				if (s<0) error = t("Speed must not be less than zero!");
				if (error == null) {
					this.maxSpeed = s;
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
