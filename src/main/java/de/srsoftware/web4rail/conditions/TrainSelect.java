package de.srsoftware.web4rail.conditions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public class TrainSelect extends Condition {
	
	private static final Object TRAIN = Train.class.getSimpleName();
	private Train train;

	@Override
	public boolean fulfilledBy(Context context) {
		return (context.train() == train) != inverted;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(train)) json.put(REALM_TRAIN, train.id());
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		train(Train.get(new Id(""+json.get(REALM_TRAIN))));
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select train")+":",Train.selector(train, null));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == train) train = null;
		super.removeChild(child);
	}
	
	@Override
	public String toString() {
		if (train == null) return "["+t("Click here to select train!")+"]";
		return t("Train")+ (inverted?"≠":"=") + train;
	}
	
	private TrainSelect train(Train train) {
		this.train = train;
		return this;
	}


	@Override
	protected Object update(Params params) {
		if (!params.containsKey(TRAIN)) return t("No train id passed to TrainSelect.update()!");
		Id tid = new Id(params.getString(TRAIN));
		Train train = Train.get(tid);
		if (train == null) return t("No train with id {} found!",tid);
		this.train = train;
		return super.update(params);
	}
}
