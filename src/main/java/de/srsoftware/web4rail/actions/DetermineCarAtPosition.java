package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class DetermineCarAtPosition extends Action {
		
	private static final String POSITION = "position";

	public DetermineCarAtPosition(BaseClass parent) {
		super(parent);
	}

	private int position = 1;
	
	@Override
	public boolean fire(Context context) {
		if (isNull(context)) return false;
		Train train = context.train();
		if (isNull(train)) return false;
		List<Car> cars = train.cars();
		Car car = null;
		if (position > 0 && position <= cars.size()) {
			car = cars.get(position-1);
		}
		if (position < 0 && -position <= cars.size()) {
			car = cars.get(cars.size()+position);
		}
		if (isNull(car)) return false;
		context.car(car);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(POSITION, position);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(POSITION)) position = json.getInt(POSITION);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Position in train"),new Input(POSITION).numeric());
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	public String toString() {
		return t("Determine, which car is at position {} of the current train",position);
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(POSITION)) {
			position = Integer.parseInt(POSITION);
		}
		return context().properties();
	}
}
