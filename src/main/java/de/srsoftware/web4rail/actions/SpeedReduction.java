package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class SpeedReduction extends Action{

	public static final String MAX_SPEED = "max_speed";
	private int maxSpeed = -1;

	public SpeedReduction(int kmh) {
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
	
	public static Object propForm(ActionList actionList, HashMap<String, String> params) {
		String error = null;
		String ms = params.get(MAX_SPEED);
		if (ms == null) {
			ms = ""+128;
		} else {
			try {
				int s = Integer.parseInt(ms);
				if (s<0) error = t("Speed must not be less than zero!");
				if (error == null) {
					actionList.add(new SpeedReduction(s));
					return t("Action added!");
				}
			} catch (NumberFormatException e) {
				error = t("Not a valid number!");
			}
		}
		Window win = Action.propForm(params);
		String formId = "edit-speedreduction";
		Tag form = new Form(formId);
		new Input(REALM, REALM_ACTIONS).hideIn(form);
		new Input(ID,actionList.id()).hideIn(form);
		new Input(ACTION,ACTION_ADD).hideIn(form);
		new Input(TYPE,SpeedReduction.class.getSimpleName()).hideIn(form);
		new Input(MAX_SPEED, ms).addTo(new Label("new speed")).addTo(form);
		//if (error != null) new Tag("div").content(error).addTo(form); 
		new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public String toString() {
		return t("Reduce speed to {} km/h",maxSpeed);
	}
}
