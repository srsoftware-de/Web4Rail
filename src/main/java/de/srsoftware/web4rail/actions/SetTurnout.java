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
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class SetTurnout extends Action {
	
	public SetTurnout(BaseClass parent) {
		super(parent);
	}

	private Turnout turnout = null;
	private Turnout.State state = State.STRAIGHT;

	@Override
	public boolean fire(Context context,Object cause) {
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
		Id turnoutId = json.has(TURNOUT) ? new Id(json.getString(TURNOUT)) : null;
		if (isSet(turnoutId)) {
			turnout = BaseClass.get(turnoutId);
			if (isNull(turnout)) new DelayedExecution(this) {
				
				@Override
				public void execute() {
					turnout = BaseClass.get(turnoutId);
				}
			};
		}
		if (json.has(Turnout.STATE)) state = Turnout.State.valueOf(json.getString(Turnout.STATE));
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {

		formInputs.add(t("Turnout")+": "+(isNull(turnout) ? t("unset") : turnout),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,TURNOUT)));
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
		if (params.containsKey(TURNOUT)) {
			BaseClass object = BaseClass.get(new Id(params.get(TURNOUT)));
			if (object instanceof Turnout) {
				turnout = (Turnout) object;
			} else return t("Clicked tile is not a {}!",t("turnout"));
		}
		String st = params.get(Turnout.STATE);
		if (isSet(st)) state = Turnout.State.valueOf(st);
		return super.update(params);
	}
}
