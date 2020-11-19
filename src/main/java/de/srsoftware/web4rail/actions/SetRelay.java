package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Relay;

public class SetRelay extends Action {
	
	private static final String RELAY = "relay";
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
			relay = Relay.get(relayId);
			state = json.getBoolean(Relay.STATE);
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
		
		Select select = new Select(RELAY);
		for (Relay relay : Relay.list()) {
			Tag option = select.addOption(relay.id(),relay.title());
			if (relay == this.relay) option.attr("selected", "selected");
		}
		select.addTo(new Label(t("Select relay:")+NBSP)).addTo(form);
		
		Select state = new Select(Relay.STATE);
		state.addOption(true,isNull(relay)?Relay.DEFAULT_LABEL_A:relay.stateLabelA);
		state.addOption(false,isNull(relay)?Relay.DEFAULT_LABEL_B:relay.stateLabelB);
		state.addTo(new Label(t("Select state:")+NBSP)).addTo(form);
		
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}

	public String toString() {
		if (isNull(relay)) return "["+t("click here to setup relay")+"]";
		return t("Set {} to {}",relay,state?relay.stateLabelA:relay.stateLabelB);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String relayId = params.get(RELAY);
		relay = Relay.get(relayId);
		String st = params.get(Relay.STATE);
		if (isSet(st)) state = st.equals("true");
		return properties(params);
	}
}
