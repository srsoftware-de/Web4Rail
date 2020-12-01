package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public class SetContextTrain extends Action {
		
	private Train train = null;
	
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
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		
		Select select = Train.selector(train, null);
		select.addTo(new Label(t("Select train:")+NBSP)).addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	public String toString() {
		return isSet(train) ? t("Set {} as context",train) : "["+t("Click here to select train!")+"]";
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id trainId = Id.from(params,Train.class.getSimpleName());
		if (isSet(trainId)) train = Train.get(trainId);
		return properties(params);
	}

}
