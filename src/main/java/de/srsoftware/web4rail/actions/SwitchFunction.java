package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Select;

public class SwitchFunction extends Action {

	private static final String FUNCTION = "function";
	private static final String EFFECT = "effect";
	private static final int TOGGLE = -1;
	private static final int ON = 1;
	private static final int OFF = 0;
	
	private int function = 1,effect=-1;
	
	public SwitchFunction(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		if (isNull(context) || isNull(context.train())) return false;
		switch (effect) {
			case TOGGLE:
				context.train().toggleFunction(function);
				return true;
			case ON:
				context.train().setFunction(function, true);
				return true;
			case OFF:
				context.train().setFunction(function, false);
				return true;
		}
		
		return false;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(EFFECT, effect);
		json.put(FUNCTION, function);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(EFFECT)) effect = json.getInt(EFFECT);
		if (json.has(FUNCTION)) function = json.getInt(FUNCTION);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		
		Select selector = new Select(FUNCTION);
		for (int i=1; i<5;i++) {
			Tag option = selector.addOption(i,t("F"+i));
			if (function == i) option.attr("selected", "selected");
		}
		formInputs.add(t("Function"), selector);
		
		Tag radioGroup = new Tag("span");
		new Radio(EFFECT, TOGGLE, t("Toggle"), effect == TOGGLE).addTo(radioGroup);
		new Radio(EFFECT, ON, t("On"), effect == ON).addTo(radioGroup);
		new Radio(EFFECT, OFF, t("Off"), effect == OFF).addTo(radioGroup);
		formInputs.add(t("Effect"),radioGroup);
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		switch (effect) {
			case TOGGLE:				
				return t("toggle {}","F"+function);
			case ON:
				return t("enable {}","F"+function);
			case OFF:
				return t("disable {}","F"+function);
		}
		return null;
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		String fn = params.get(FUNCTION);
		if (isSet(fn)) {
			function = Integer.parseInt(fn);
			if (function < 1 || function > 4) function = 1;
		}
		String effect = params.get(EFFECT);
		if (isSet(effect)) switch (effect) {
		case "1":
		case "0":
		case "-1":
			this.effect = Integer.parseInt(effect);
		}
		return super.update(params);
	}
}
