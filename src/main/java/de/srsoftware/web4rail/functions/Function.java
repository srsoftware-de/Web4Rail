package de.srsoftware.web4rail.functions;

import java.util.List;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public abstract class Function extends BaseClass{
	
	public static final String NEW = "new_function";
	private static final String PACKAGE = Function.class.getPackageName();
	private static final String INDEX = "index";
	static final String FORWARD = "forward";
	static final String REVERSE = "reverse";
	
	private int decoderFunction = 1; 

	public static Tag selector() {
		Select selector = new Select(NEW);
		selector.addOption("", t("Select function"));
		
		for (Class<? extends Function> fun : List.of(HeadLight.class,TailLight.class,InteriorLight.class,Coupler.class,CustomFunction.class)) {
			String className = fun.getSimpleName();
			selector.addOption(className,t(className));
		}
		
		return selector;
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

	private String name() {
		return t(getClass().getSimpleName());
	}
	
	@Override
	public Object update(Params params) {
		if (params.containsKey(INDEX)) decoderFunction = params.getInt(INDEX);
		return super.update(params);
	}
}
