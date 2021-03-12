package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class SplitTrain extends Action {
	
	private static final String POSITION = "position";
	private int position = 1;

	public SplitTrain(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		Train train = context.train();
		if (isNull(train)) return false;		
		return train.splitAfter(position);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(POSITION, position);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(POSITION)) position = json.getInt(POSITION);
		return super.load(json);
	}
	
	@Override
	public String toString() {
		return t("Split train behind {} cars",position);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Split behind"),new Input(POSITION, position).numeric().addTo(new Tag("span")).content(t("&nbsp;cars")));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(POSITION)) position = Integer.parseInt(params.get(POSITION));
		return super.update(params);
	}
}
