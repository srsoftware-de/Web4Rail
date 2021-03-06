package de.srsoftware.web4rail.conditions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.Tile;

public class SwitchIsOn extends Condition {
	
	private Switch swtch = null;
	private static final String SWITCH = "switch";
	
	

	@Override
	public boolean fulfilledBy(Context context) {
		if (isSet(swtch)) return swtch.isOn() != inverted;
		return false;
	}
	
	@Override
	public void inversionOption(FormInput formInputs) {
		Tag radios = new Tag("div");
		new Radio(INVERTED, "off", t("{} is on",swtch), !inverted).addTo(radios);
		new Radio(INVERTED, "on", t("{} is off",swtch), inverted).addTo(radios);
		formInputs.add(t("Condition"), radios);
	}

	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(swtch)) json.put(SWITCH, swtch.id().toString());		
		return json;
	}
	
	public Condition load(JSONObject json) {
		if (json.has(SWITCH)) new LoadCallback() {			
			@Override
			public void afterLoad() {
				swtch = BaseClass.get(Id.from(json,SWITCH));
			}
		};
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select switch")+": "+(isNull(swtch) ? t("unset") : swtch),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,SWITCH)));

		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isNull(swtch))  return "["+t("Click here to select switch!")+"]";
		return t(inverted ? "{} is off" : "{} is on",swtch) ;
	}

	@Override
	protected Object update(Params params) {
		String switchId = params.getString(SWITCH);
		Tile tile = isSet(switchId) ? BaseClass.get(new Id(switchId)) : null;
		if (tile instanceof Switch) swtch = (Switch) tile; 
		return super.update(params);
	}
}
