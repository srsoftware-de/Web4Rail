package de.srsoftware.web4rail.functions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Constants;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public abstract class Function extends BaseClass{
	
	public static final String SELECTOR = "selected_fun";
	private static final String PACKAGE = Function.class.getPackageName();
	private static final String INDEX = "index";
	static final String FORWARD = "forward";
	static final String REVERSE = "reverse";
	
	private int decoderFunction = 1;
	private boolean enabled; 

	public static Object action(Params params) {
		String action = params.getString(ACTION);
		Function function = BaseClass.get(Id.from(params));
		BaseClass parent = isSet(function) ? function.parent() : null;
		switch (action) {
			case ACTION_DROP:
				if (isSet(function)) {
					function.remove();
					return parent.properties();
				}
		}
		String message = t("Unknown action: {}",params.get(Constants.ACTION));
		return isSet(parent) ? parent.properties(message) : message;
	}

	public static Function create(String className) {
		if (isNull(className)) return null;
		try {
			return (Function) Class.forName(PACKAGE+"."+className).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;	
	}
	
	public boolean enabled() {
		return enabled;
	}
	
	public boolean enabled(Decoder decoder) {
		return enabled;
	}

	public Fieldset form(Decoder decoder) {
		Fieldset fieldset = new Fieldset(name());
		String prefix = "functions/"+id()+"/";
		Select select = new Select(prefix+"index");
		for (int i=1; i<=decoder.numFunctions(); i++) {
			Tag option = select.addOption(i);
			if (i == decoderFunction) option.attr("selected", "selected");
		}
		select.addTo(new Label(t("Decoder function")+COL)).addTo(fieldset);
				
		return fieldset;		
	}
	
	public int index() {
		return decoderFunction;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TYPE, type());
		json.put(INDEX, decoderFunction);
		return json;
	}
	
	@Override
	public Function load(JSONObject json) {
		super.load(json);
		if (json.has(INDEX)) decoderFunction = json.getInt(INDEX);
		return this;
	}

	public String name() {
		return t(type());
	}
	
	public static Select selector() {
		return selector(null);
	}
	
	public static Select selector(String preselect) {
		Select selector = new Select(SELECTOR);
		selector.addOption("", "["+t("Select function")+"]");
		
		for (Class<? extends Function> fun : List.of(HeadLight.class,TailLight.class,InteriorLight.class,Coupler.class,CustomFunction.class)) {
			String className = fun.getSimpleName();
			String name = t(className);
			Tag option = selector.addOption(className,name);
			if (name.equals(t(preselect))) option.attr("selected", "selected");
		}
		selector.children().sort((c1,c2) -> c1.children().toString().compareToIgnoreCase(c2.children().toString()));
		return selector;
	}

	public Function setState(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	@Override
	public String toString() {
		return name()+"("+decoderFunction+"="+enabled+")";
	}
	
	protected String type() {
		return getClass().getSimpleName();
	}
	
	@Override
	public Object update(Params params) {
		if (params.containsKey(INDEX)) decoderFunction = params.getInt(INDEX);
		return super.update(params);
	}
}
