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
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;

public class SetSignal extends Action {
	
	public SetSignal(BaseClass parent) {
		super(parent);
	}

	private static final String SIGNAL = "signal";
	private Signal signal = null;
	private String state = Signal.RED;

	@Override
	public boolean correspondsTo(Action other) {
		if (other instanceof SetSignal) {
			SetSignal otherSS = (SetSignal) other;
			return otherSS.signal == this.signal;
		}
		return false;
	}
	
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Signal")+": "+(isNull(signal) ? t("unset") : signal),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,SIGNAL)));
		Select state = new Select(Signal.STATE);
		for (String st:Signal.knownStates) {
			Tag option = state.addOption(st);
			if (st.equals(this.state)) option.attr("selected", "selected");
		}
		formInputs.add(t("Select state"),state);

		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == signal) signal = null;
		super.removeChild(child);
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
	public Object update(HashMap<String, String> params) {
		String signalId = params.get(SIGNAL);
		Id tileId = isSet(signalId) ? new Id(signalId) : null;
		Tile tile = isSet(tileId) ? plan.get(tileId, false) : null;
		if (tile instanceof Signal) signal = (Signal) tile;
		String st = params.get(Signal.STATE);
		if (isSet(st)) state = st;
		return super.update(params);
	}
}
