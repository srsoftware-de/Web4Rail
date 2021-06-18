package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.functions.CustomFunction;
import de.srsoftware.web4rail.functions.Function;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public class SwitchFunction extends Action {

	private static final String FUNCTION = "function";
	private static final String EFFECT = "effect";
	private static final int TOGGLE = -1;
	private static final int ON = 1;
	private static final int OFF = 0;
	
	private int effect=-1;
	private String function = "["+t("Select function")+"]";
	
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
		try {
			if (json.has(EFFECT)) effect = json.getInt(EFFECT);
			if (json.has(FUNCTION)) function = json.getString(FUNCTION);
		} catch(JSONException je) {
			LOG.warn("Was not able to load function!",je);
		}
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		
		Select selector = Function.selector(function);
		Vector<Tag> options = selector.children();
		for (Tag option : options) { // remove unconfigured CustomFunction
			if (CustomFunction.class.getSimpleName().equals(option.get("value"))) {
				options.remove(option);
				break;
			}
		}
		List<CustomFunction> customFunctions = BaseClass.listElements(CustomFunction.class);
		for (CustomFunction cf : customFunctions) { // add configured custom functions
			String cfName = cf.name();
			Tag option = selector.addOption(cfName);
			if (function.equals(cfName)) option.attr("selected", "selected");
		}
		formInputs.add(t("Function"), selector);
		
		Tag radioGroup = new Tag("span");
		new Radio(EFFECT, TOGGLE, t("Toggle"), effect == TOGGLE).addTo(radioGroup);
		new Radio(EFFECT, ON, t("On"), effect == ON).addTo(radioGroup);
		new Radio(EFFECT, OFF, t("Off"), effect == OFF).addTo(radioGroup);
		formInputs.add(t("Effect"),radioGroup);
		
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		switch (effect) {
			case TOGGLE:				
				return t("toggle {}",function);
			case ON:
				return t("enable {}",function);
			case OFF:
				return t("disable {}",function);
		}
		return null;
	}
	
	@Override
	protected Object update(Params params) {
		String fn = params.getString(Function.SELECTOR);
		if (isSet(fn) && !fn.isEmpty()) function = t(fn);
		String effect = params.getString(EFFECT);
		if (isSet(effect)) switch (effect) {
		case "1":
		case "0":
		case "-1":
			this.effect = Integer.parseInt(effect);
		}
		return super.update(params);
	}
}
