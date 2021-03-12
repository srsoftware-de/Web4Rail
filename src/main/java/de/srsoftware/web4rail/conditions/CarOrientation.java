package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

public class CarOrientation extends Condition {
	
	private static final String ORIENTATION = "orientation";
	private static final String CAR = "car";
	private static final String POSITION = "position";
	private boolean orientation = Car.FORWARD;
	private int position = 1;
	private Car car;
	
	@Override
	public boolean fulfilledBy(Context context) {
		Car car = this.car;
		if (isNull(car)) {
			Train train = context.train();
			if (position == 0 || isNull(train)) return false;
			List<Car> cars = train.cars();
			if (position > 0) {
				if (position>cars.size()) return false;
				car = cars.get(position-1);				
			} else {
				if (-position>cars.size()) return false;
				car = cars.get(cars.size()+position);				
			}
		}
		if (isNull(car)) return false;
		return inverted ? car.orientation() != orientation : car.orientation() == orientation;		
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json().put(ORIENTATION, orientation);
		if (isSet(car)) json.put(CAR, car.id().toString());
		json.put(POSITION,position);
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(CAR)) car = BaseClass.get(new Id(json.getString(CAR)));
		if (json.has(ORIENTATION)) orientation = json.getBoolean(ORIENTATION);
		if (json.has(POSITION)) position = json.getInt(POSITION);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select car"),Car.selector(isSet(car) ? car : t("Car of train"), null));
		formInputs.add(t("If car of train: inspect car number"),new Input(POSITION, position).numeric().addTo(new Tag("span")).content(NBSP+"("+t("Use negative number to count from end.")+")"));
		
		Tag radioGroup = new Tag("span");
		new Radio(ORIENTATION, "f", t("forward"), orientation).addTo(radioGroup);
		new Radio(ORIENTATION, "r", t("revers"), !orientation).addTo(radioGroup);

		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		String c = isSet(car) ? car.toString() : t("Car {} of train",position);
		return t("{} is oriented {}",c,inverted ? t("backward") : t("forward"));
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
		if (params.containsKey(POSITION)) position = Integer.parseInt(params.get(POSITION));
		return super.update(params);
	}
}
