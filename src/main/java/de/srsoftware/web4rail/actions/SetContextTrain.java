package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public class SetContextTrain extends Action {
		
	private Train train = null;

	public SetContextTrain(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context,Object cause) {
		context.train(train);		
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(train)) json.put(REALM_TRAIN, train.id());
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
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select train"),Train.selector(train, null));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == train) train = null;
		super.removeChild(child);
	}
	
	public String toString() {
		return isSet(train) ? t("Set {} as context",train) : "["+t("Click here to select train!")+"]";
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		Id trainId = Id.from(params,Train.class.getSimpleName());
		if (isSet(trainId)) train = Train.get(trainId);
		return super.update(params);
	}

}
