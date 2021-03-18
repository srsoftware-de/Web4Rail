package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public class CarInTrain extends Condition {
	
	private static final String CAR = "car";
	private Car car;
	
	@Override
	public boolean fulfilledBy(Context context) {
		Train train = context.train();
		if (isNull(train) || isNull(car)) return false;		
		boolean contained = train.cars().contains(car);
		return inverted ? !contained : contained;		
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(car)) json.put(CAR, car.id().toString());		
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(CAR)) car = BaseClass.get(new Id(json.getString(CAR)));
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select car"),Car.selector(car, null));
		
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isNull(car))  return "["+t("Click here to select car!")+"]";
		return t(inverted ? "train does not contain {}" : "train contains {}",car) ;
	}

	@Override
	protected Object update(HashMap<String, String> params) {
		String carId = params.get(Car.class.getSimpleName());
		if (isSet(carId)) car = BaseClass.get(new Id(carId));
		return super.update(params);
	}
}
