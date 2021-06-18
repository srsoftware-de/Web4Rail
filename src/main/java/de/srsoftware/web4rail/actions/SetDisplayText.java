package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.TextDisplay;
import de.srsoftware.web4rail.tiles.Tile;

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
	protected String highlightId() {
		return isSet(display) ? display.id().toString() : null;
	}
	
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(display)) json.put(DISPLAY, display.id());
		return json;
	}

	@Override
	protected Label label() {
		return new Label(t("Text to show on display")+COL);
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(DISPLAY)) new LoadCallback() {
			@Override
			public void afterLoad() {
				display = (TextDisplay) plan.get(Id.from(json,DISPLAY), false);
			}
		};			
		return super.load(json);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == display) display = null;
		super.removeChild(child);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Display")+": "+(isNull(display) ? t("unset") : display),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,DISPLAY)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
		
	@Override
	public String toString() {
		return isNull(display) ? "["+t("Click here to select display!")+"]" : t("Display \"{}\" on {}.",text,display);
	}
	
	@Override
	protected Object update(Params params) {
		if (params.containsKey(DISPLAY)) {
			Tile object = plan.get(new Id(params.getString(DISPLAY)), true);
			if (object instanceof TextDisplay) {
				display = (TextDisplay) object;
			} else return t("Clicked tile is not a {}!",t("display"));

		}
		return super.update(params);
	}
}
