package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tiles.Contact;

public class SpeedReduction extends Action{

	public static final String MAX_SPEED = "max_speed";
	private int maxSpeed;

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
	
	public static Window propForm(HashMap<String, String> params, Route route, Contact contact) {
		String error = null;
		String ms = params.get(MAX_SPEED);
		if (ms == null) {
			ms = ""+128;
		} else {
			try {
				int s = Integer.parseInt(ms);
				if (s<0) error = t("Speed must not be less than zero!");
				if (error == null) {
					route.addAction(contact.trigger(),new SpeedReduction(s));
					contact.plan().stream("Action added!");
					return route.properties();
				}
			} catch (NumberFormatException e) {
				error = t("Not a valid number!");
			}
		}
		Window win = Action.propForm(params);
		String formId = "add-action-to-contact-"+contact.id();
		Tag form = new Form(formId);
		new Tag("div").content(t("Add Action {} to contact {} on route {}:",SpeedReduction.class.getSimpleName(),contact,route)).addTo(win);
		new Input(REALM, REALM_ROUTE).hideIn(form);
		new Input(ID,route.id()).hideIn(form);
		new Input(ACTION,ACTION_ADD_ACTION).hideIn(form);
		new Input(CONTACT,contact.id()).hideIn(form);
		new Input(TYPE,SpeedReduction.class.getSimpleName()).hideIn(form);
		new Input(MAX_SPEED, ms).addTo(new Label("new speed")).addTo(form);
		if (error != null) new Tag("div").content(error).addTo(form); 
		new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public String toString() {
		return t("Reduce speed to {} km/h",maxSpeed);
	}
}
