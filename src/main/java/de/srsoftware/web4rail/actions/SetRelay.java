package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Relay;

public class SetRelay extends Action {
	
	public SetRelay(BaseClass parent) {
		super(parent);
	}

	private Relay relay = null;
	private boolean state = false;

	@Override
	public boolean fire(Context context) {
		if (isNull(relay)) return false;
		relay.state(state);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(relay)) {
			json.put(RELAY, relay.id());
			json.put(Relay.STATE, state);
		}
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		String relayId = json.getString(RELAY);
		if (isSet(relayId)) {
			relay = Relay.get(new Id(relayId));
			state = json.getBoolean(Relay.STATE);
		}
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {

		formInputs.add(t("Select relay"),Relay.selector(relay,null));
		
		Select state = new Select(Relay.STATE);
		state.addOption(true,isNull(relay)?Relay.DEFAULT_LABEL_A:relay.stateLabelA);
		state.addOption(false,isNull(relay)?Relay.DEFAULT_LABEL_B:relay.stateLabelB);
		formInputs.add(t("Select state"),state);
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	public String toString() {
		if (isNull(relay)) return "["+t("click here to setup relay")+"]";
		return t("Set {} to {}",relay,state?relay.stateLabelA:relay.stateLabelB);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id relayId = new Id(params.get(RELAY));
		relay = Relay.get(relayId);
		String st = params.get(Relay.STATE);
		if (isSet(st)) state = st.equals("true");
		return properties();
	}
}
