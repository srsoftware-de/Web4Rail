package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.DelayedExecution;
import de.srsoftware.web4rail.tiles.Relay;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.Tile;

public class SetRelayOrSwitch extends Action {
	
	private static final String SWITCH = "switch";

	public SetRelayOrSwitch(BaseClass parent) {
		super(parent);
	}

	private Tile relayOrSwitch = null;
	private boolean state = false;

	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(relayOrSwitch)) return false;
		if (relayOrSwitch instanceof Relay)	((Relay)relayOrSwitch).state(state);
		if (relayOrSwitch instanceof Switch) ((Switch)relayOrSwitch).state(state);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (relayOrSwitch instanceof Relay)	{
			json.put(RELAY, relayOrSwitch.id());
			json.put(STATE, state);
		}
		if (relayOrSwitch instanceof Switch)	{
			json.put(SWITCH, relayOrSwitch.id());
			json.put(STATE, state);
		}
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(RELAY)) {
			String relayId = json.getString(RELAY);
			relayOrSwitch = BaseClass.get(new Id(relayId));
			if (isNull(relayOrSwitch)) new DelayedExecution(this) {
				
				@Override
				public void execute() {
					relayOrSwitch = BaseClass.get(new Id(relayId));
				};
			};
		}
		if (json.has(SWITCH)) {
			String relayId = json.getString(SWITCH);
			relayOrSwitch = BaseClass.get(new Id(relayId));
			if (isNull(relayOrSwitch)) new DelayedExecution(this) {
				
				@Override
				public void execute() {
					relayOrSwitch = BaseClass.get(new Id(relayId));
				}
			};
		}
		if (json.has(STATE)) state = json.getBoolean(STATE);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag span = new Tag("span");
		if (isSet(relayOrSwitch)) span.content(relayOrSwitch+NBSP);
		button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,Relay.class.getSimpleName())).addTo(span);
		formInputs.add(t("Select relay"),span);
		Select state = new Select(Relay.STATE);
		if (relayOrSwitch instanceof Relay) {
			Relay relay = (Relay) relayOrSwitch;
			state.addOption(true,relay.stateLabelA);
			state.addOption(false,relay.stateLabelB);
		}
		if (relayOrSwitch instanceof Switch) {
			state.addOption(true,t("On"));
			state.addOption(false,t("Off"));
		}
		formInputs.add(t("Select state"),state);
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == relayOrSwitch) relayOrSwitch = null;
		super.removeChild(child);
	}
	
	public String toString() {
		if (isNull(relayOrSwitch)) return "["+t("click here to setup relay or switch")+"]";
		if (relayOrSwitch instanceof Relay) {
			Relay relay = (Relay) relayOrSwitch;
			return t("Set {} to {}",relayOrSwitch,state?relay.stateLabelA:relay.stateLabelB);
		}
		return t("Set {} to {}",relayOrSwitch,state?t("On"):t("Off"));
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String tileId = params.get(Relay.class.getSimpleName());
		Tile tile = isSet(tileId) ? BaseClass.get(new Id(tileId)) : relayOrSwitch;
		if (tile instanceof Relay || tile instanceof Switch) {
			relayOrSwitch = tile;
		}
		String st = params.get(Relay.STATE);
		if (isSet(st)) state = st.equals("true");
		return context().properties();
	}
}
