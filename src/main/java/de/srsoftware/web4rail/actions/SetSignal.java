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
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;

public class SetSignal extends Action {
	
	private static final String SIGNAL = "signal";
	private Signal signal = null;
	private String state = Signal.STOP;

	@Override
	public boolean fire(Context context) {
		if (isNull(signal)) return false;
		return signal.state(state);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(signal)) {
			json.put(SIGNAL, signal.id());
			json.put(Signal.STATE, state);
		}
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		Tile tile = plan.get(new Id(json.getString(SIGNAL)), false);
		if (tile instanceof Signal) signal = (Signal) tile;
		state = json.getString(Signal.STATE);
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
		
		Select select = new Select(SIGNAL);
		for (Signal signal : plan.signals()) {
			Tag option = select.addOption(signal.id(),signal.title());
			if (signal == this.signal) option.attr("selected", "selected");
		}
		select.addTo(new Label(t("Select signal:")+NBSP)).addTo(form);
		
		Select state = new Select(Signal.STATE);
		for (String st:Signal.knownStates) {
			Tag option = state.addOption(st);
			if (st.equals(this.state)) option.attr("selected", "selected");
		}
		state.addTo(new Label(t("Select state:")+NBSP)).addTo(form);
		
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	public SetSignal set(Signal sig) {
		signal = sig;
		return this;
	}

	public SetSignal to(String state) {
		this.state = state;
		return this;
	}

	public String toString() {
		if (isNull(signal)) return "["+t("click here to setup signal")+"]";
		return t("Set {} to {}",signal,state);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Tile tile = plan.get(new Id(params.get(SIGNAL)), false);
		if (tile instanceof Signal) signal = (Signal) tile;
		String st = params.get(Signal.STATE);
		if (isSet(st)) state = st;
		return properties(params);
	}


}
