package de.srsoftware.web4rail.conditions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action.Context;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TrainSelect extends Condition {
	
	private static final Object TRAIN = Train.class.getSimpleName();
	private Train train;

	@Override
	public boolean fulfilledBy(Context context) {
		return context.train == train;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(REALM_TRAIN, train.id);
	}
	
	public static TrainSelect load(JSONObject json) {
		int trainId = json.getInt(REALM_TRAIN);
		return new TrainSelect().train(Train.get(trainId));		
	}
	
	@Override
	protected Window properties(HashMap<String, String> params) {
		Window win = new Window("condition-props", t("Properties of {}",getClass().getSimpleName()));
		String formId = "conditional-props-"+id;
		Form form = new Form(formId);
		new Input(REALM,REALM_CONDITION).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(ID,id).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		Train.selector(train, null).addTo(new Label(t("Select train:")+NBSP)).addTo(form);
		new Button(t("Apply"),"return submitForm('"+formId+"');").addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public String toString() {
		if (train == null) return super.toString();
		return t("Train = {}",train);
	}
	
	private TrainSelect train(Train train) {
		this.train = train;
		return this;
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(TRAIN)) return t("No train id passed to TrainSelect.update()!");
		int tid = Integer.parseInt(params.get(TRAIN));
		Train train = Train.get(tid);
		if (train == null) return t("No train with id {} found!",tid);
		this.train = train;
		return t("Updated condition");
	}
}
