package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;

public class CoupleTrain extends Action {
	
	private static final String LAST = "last";
	private static final String SWAP = "swap";
	private boolean last = false;
	private boolean swap = false;

	public CoupleTrain(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		Train train = context.train();
		if (isNull(train)) return false;		
		Block block = train.currentBlock();
		if (isNull(block)) return false;
		Train parkingTrain = block.parkedTrain(last);
		if (isNull(parkingTrain)) return false;
		train.coupleWith(parkingTrain,swap);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (last) json.put(LAST, last);
		if (swap) json.put(SWAP, swap);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(LAST)) last = json.getBoolean(LAST);
		if (json.has(SWAP)) swap = json.getBoolean(SWAP);
		return super.load(json);
	}
	
	@Override
	public String toString() {
		return last ? t("Couple last parked train") : t("Couple first parked train");
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Couple"),new Checkbox(LAST, t("last parked train"), last));
		formInputs.add(t("Swap order"),new Checkbox(SWAP, t("Swap order of trains"), swap));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		last = "on".equals(params.get(LAST));
		swap = "on".equals(params.get(SWAP));
		return super.update(params);
	}
}
