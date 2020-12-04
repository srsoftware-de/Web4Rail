package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;

public class SetContextTrain extends Action {
		
	private Train train = null;

	public SetContextTrain(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
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
		super.load(json);
		if (json.has(REALM_TRAIN)) {
			new Thread() { // load asynchronously, as referred tile may not be available,yet
				public void run() {
					try {
						sleep(1000);
						Id trainId = Id.from(json,REALM_TRAIN);
						if (isSet(trainId)) train = Train.get(trainId);
					} catch (InterruptedException e) {}						
				};
			}.start();
		}
		return this;
	}
	
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select train"),Train.selector(train, null));
		return super.properties(preForm, formInputs, postForm);
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
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id trainId = Id.from(params,Train.class.getSimpleName());
		if (isSet(trainId)) train = Train.get(trainId);
		return super.update(params);
	}

}
