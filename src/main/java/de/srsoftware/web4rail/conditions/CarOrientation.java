package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;

public class CarOrientation extends Condition {
	
	private static final String ORIENTATION = "orientation";
	private static final String CAR = "car";
	private boolean orientation = Car.FORWARD;
	private Car car;
	
	@Override
	public boolean fulfilledBy(Context context) {
		return inverted ? car.orientation() != orientation : car.orientation() == orientation;		
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json().put(ORIENTATION, orientation);
		if (isSet(car)) json.put(CAR, car.id().toString());		
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(CAR)) car = BaseClass.get(new Id(json.getString(CAR)));
		if (json.has(ORIENTATION)) orientation = json.getBoolean(ORIENTATION);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select car"),Car.selector(car, null));
		
		Tag radioGroup = new Tag("span");
		new Radio(ORIENTATION, "f", t("forward"), orientation).addTo(radioGroup);
		new Radio(ORIENTATION, "r", t("revers"), !orientation).addTo(radioGroup);
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return t("{} is oriented {}",car,inverted ? t("backward") : t("forward"));
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String or = params.get(ORIENTATION);
		if (isSet(or)) switch (or){
			case "f":
				orientation = Car.FORWARD;
				break;
			case "r":
				orientation = Car.REVERSE;
				break;
		}
		String carId = params.get(Car.class.getSimpleName());
		if (isSet(carId)) car = BaseClass.get(new Id(carId));
		return super.update(params);
	}
}
