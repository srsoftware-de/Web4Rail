package de.srsoftware.web4rail.conditions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Label;

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
	public Tag propForm(HashMap<String, String> params) {
		Tag form = super.propForm(params);
		Train.selector(train, null).addTo(new Label(t("Select train:")+NBSP)).addTo(form);
		return form;
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
