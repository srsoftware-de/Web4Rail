package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Decoupler;
import de.srsoftware.web4rail.tiles.Tile;

public class EngageDecoupler extends Action {
	
	private static final String DECOUPLER = Decoupler.class.getSimpleName();

	public EngageDecoupler(BaseClass parent) {
		super(parent);
	}

	private Decoupler decoupler = null;

	@Override
	public boolean fire(Context context) {
		if (isNull(decoupler)) return false;
		decoupler.engage();
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(decoupler)) {
			json.put(Decoupler.class.getSimpleName(), decoupler.id());
		}
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(DECOUPLER)) {
			String decouplerId = json.getString(DECOUPLER);
			decoupler = BaseClass.get(new Id(decouplerId));
			if (isNull(decoupler)) Application.threadPool.execute(new Thread() { // if relay not loaded, yet: wait one sec and try again
				public void run() {
					try {
						sleep(1000);
					} catch (InterruptedException e) {}
					decoupler = BaseClass.get(new Id(decouplerId));
				};
			});
		}
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Decoupler")+": "+(isNull(decoupler) ? t("unset") : decoupler),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,DECOUPLER)));
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == decoupler) decoupler = null;
		super.removeChild(child);
	}
	
	public String toString() {
		if (isNull(decoupler)) return "["+t("Click here to setup decoupler")+"]";
		return t("Engage {}",decoupler);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(DECOUPLER)) {
			Tile tile = BaseClass.get(new Id(params.get(DECOUPLER)));
			if (tile instanceof Decoupler) {
				decoupler = (Decoupler) tile;				
			} else return t("Clicked tile is not a {}!",t("decoupler"));
		}
		return context().properties();
	}
}
