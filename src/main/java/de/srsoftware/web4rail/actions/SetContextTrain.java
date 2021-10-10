package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public class SetContextTrain extends Action {
		
	private Train train = null;
	private Car car = null;

	public SetContextTrain(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		Train t = isSet(train) ? train : car.train();
		if (isNull(t)) return false;
		context.train(t);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(train)) json.put(REALM_TRAIN, train.id());
		if (isSet(car)) json.put(REALM_CAR, car.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(REALM_TRAIN)) new LoadCallback() {
			@Override
			public void afterLoad() {
				train = Train.get(Id.from(json,REALM_TRAIN));
			}
		};
		if (json.has(REALM_CAR)) new LoadCallback() {
			@Override
			public void afterLoad() {
				car = Car.get(Id.from(json,REALM_CAR));
			}
		};
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select train"),Train.selector(train, null));
		formInputs.add(t("Select car"),Car.selector(car, null));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == train) train = null;
		super.removeChild(child);
	}
	
	public String toString() {
		if (isSet(train)) return t("Set {} as context",train);
		if (isSet(car)) return t("Set train of {} as context",car);
		return "["+t("Click here to select train!")+"]";
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		Id trainId = Id.from(params,Train.class.getSimpleName());
		if (isSet(trainId)) {
			Train newTrain = Train.get(trainId);
			if (newTrain != train) {
				train = newTrain;
				car = null;
				params.remove(Car.class.getSimpleName());
			}
		} 		
		
		Id carId = Id.from(params,Car.class.getSimpleName());
		if (isSet(carId) && !carId.equals(0)) {
			car = Car.get(carId);
			train = null;
		}

		
		return super.update(params);
	}

}
