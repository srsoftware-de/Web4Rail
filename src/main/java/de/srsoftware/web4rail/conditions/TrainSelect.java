package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;

public class TrainSelect extends Condition {
	
	private static final Object TRAIN = Train.class.getSimpleName();
	private Train train;

	@Override
	public boolean fulfilledBy(Context context) {
		return (context.train() == train) != inverted;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(REALM_TRAIN, train.id());
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		train(Train.get(new Id(json.getString(REALM_TRAIN))));
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select train"),Train.selector(train, null));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		if (train == null) return t("[Click here to select train!]");
		return t("Train")+ (inverted?"≠":"=") + train;
	}
	
	private TrainSelect train(Train train) {
		this.train = train;
		return this;
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(TRAIN)) return t("No train id passed to TrainSelect.update()!");
		Id tid = new Id(params.get(TRAIN));
		Train train = Train.get(tid);
		if (train == null) return t("No train with id {} found!",tid);
		this.train = train;
		return super.update(params);
	}
}
