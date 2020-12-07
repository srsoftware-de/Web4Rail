package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Turnout;

public class Loop extends ActionList {
		
	private String subject = Train.class.getSimpleName();
	private static final String SUBJECT = "subject"; 

	public Loop(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		if (isNull(subject)) return false;
		List<? extends BaseClass> elements = null;
		switch (subject) {
			case "Block":
				elements = BaseClass.listElements(Block.class);
				break;
			case "Contact":
				elements = BaseClass.listElements(Contact.class);
				break;
			case "Route":
				elements = BaseClass.listElements(Route.class);
				break;
			case "Signal":
				elements = BaseClass.listElements(Signal.class);
				break;
			case "Turnout":
				elements = BaseClass.listElements(Turnout.class);
				break;
			case "Train":
				elements = BaseClass.listElements(Train.class);
				break;			
		}
		if (elements == null) return false;
		for (BaseClass element : elements) super.fire(context.setMain(element));
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(subject)) json.put(SUBJECT, subject);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(SUBJECT)) subject = json.getString(SUBJECT); 
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select subject"),typeSelector());
		return super.properties(preForm, formInputs, postForm);
	}
	
	public String toString() {
		return t("For each {} do:",subject);
	};
	
	private Tag typeSelector() {
		Select select = new Select(SUBJECT);
		List<String> types = List.of(Block.class,Contact.class,Route.class,Signal.class,Train.class,Turnout.class)
				.stream()
				.map(cls -> cls.getSimpleName())
				.sorted((s1,s2) -> (t(s1).compareTo(t(s2))))
				.collect(Collectors.toList());
		for (String cls : types) {
			Tag option = select.addOption(cls,t(cls));
			if (cls.equals(subject)) option.attr("selected", "selected");
		}
		return select;
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String newSubject = params.get(SUBJECT);
		if (isSet(newSubject)) subject = newSubject;
		return super.update(params);
	}

}
