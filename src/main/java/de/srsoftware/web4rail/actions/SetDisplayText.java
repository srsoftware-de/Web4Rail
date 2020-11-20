package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.TextDisplay;

public class SetDisplayText extends TextAction{

	private TextDisplay display;
	private static final String DISPLAY = "display";
	
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
						display = (TextDisplay) plan.get(json.getString(DISPLAY), false);
					} catch (InterruptedException e) {}						
				};
			}.start();
		}
		return super.load(json);
	}
	
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		
		Select select = TextDisplay.selector(display, null);
		Tag label = select.addTo(new Label(t("Select display:")+NBSP));
		
		for (Tag tag : win.children()) {
			if (tag instanceof Form) {
				tag.children().insertElementAt(label, 1);
				break;
			}
		}
		return win;
	}
	
	@Override
	public String toString() {
		return isNull(display) ? t("[Click here to select display!]") : t("Display \"{}\" on {}.",text,display);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		super.update(params);
		String displayId = params.get(TextDisplay.class.getSimpleName());
		if (isSet(displayId)) display = (TextDisplay) plan.get(displayId, false);
		return properties(params);
	}
}
