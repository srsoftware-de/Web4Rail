package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tiles.TextDisplay;

public class SetDisplayText extends TextAction{

	private TextDisplay display;
	private static final String DISPLAY = "display";

	public SetDisplayText(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		plan.place(display.text(fill(text, context)));
		return true;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(DISPLAY, display.id());
	}

	@Override
	protected Label label() {
		return new Label(t("Text to show on display:")+NBSP);
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(DISPLAY)) {
			new Thread() { // load asynchronously, as referred tile may not be available,yet
				public void run() {
					try {
						sleep(1000);
						display = (TextDisplay) plan.get(Id.from(json,DISPLAY), false);
					} catch (InterruptedException e) {}						
				};
			}.start();
		}
		return super.load(json);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == display) display = null;
		super.removeChild(child);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select display"),TextDisplay.selector(display, null));
		return super.properties(preForm, formInputs, postForm);
	}
		
	@Override
	public String toString() {
		return isNull(display) ? t("[Click here to select display!]") : t("Display \"{}\" on {}.",text,display);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {		
		String displayId = params.get(TextDisplay.class.getSimpleName());
		if (isSet(displayId)) display = (TextDisplay) plan.get(new Id(displayId), false);
		return super.update(params);
	}
}
