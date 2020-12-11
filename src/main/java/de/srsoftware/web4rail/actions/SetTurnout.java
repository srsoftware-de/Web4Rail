package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class SetTurnout extends Action {
	
	public SetTurnout(BaseClass parent) {
		super(parent);
	}

	private Turnout turnout = null;
	private Turnout.State state = State.STRAIGHT;

	@Override
	public boolean fire(Context context) {
		if (isNull(turnout)) return false;		
		if (!turnout.state(state).succeeded()) return false;
		if (turnout.address() == 0) return true;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(turnout)) {
			json.put(TURNOUT, turnout.id());
			json.put(Turnout.STATE, state);
		}
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		String turnoutId = json.getString(TURNOUT);
		if (isSet(turnoutId)) {
			turnout = BaseClass.get(new Id(turnoutId));
			state = Turnout.State.valueOf(json.getString(Turnout.STATE));
		}
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {

		formInputs.add(t("Select turnout"),Turnout.selector(turnout,null));
		
		if (isSet(turnout)) {
			Select select = new Select(Turnout.STATE);
			
			for (Turnout.State st : turnout.states()) {
				Tag option = select.addOption(st,t(st.toString()));
				if (st == state) option.attr("selected", "selected");
			}
			formInputs.add(t("Select state"),select);
		}
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == turnout) turnout = null;
		super.removeChild(child);
	}
	
	public SetTurnout setState(State state) {
		this.state = state;
		return this;
	}

	public SetTurnout setTurnout(Turnout turnout) {
		this.turnout = turnout;
		return this;
	}

	public String toString() {
		if (isNull(turnout)) return "["+t("click here to setup turnout")+"]";
		return t("Set {} to {}",turnout,state);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id turnoutId = new Id(params.get(TURNOUT));
		turnout = BaseClass.get(turnoutId);
		String st = params.get(Turnout.STATE);
		if (isSet(st)) state = Turnout.State.valueOf(st);
		return super.update(params);
	}
}
