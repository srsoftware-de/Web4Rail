package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tiles.Block;

public class DetermineTrainInBlock extends Action {
		
	public DetermineTrainInBlock(Context parent) {
		super(parent);
	}

	private Block block = null;
	
	@Override
	public boolean fire(Context context) {
		context.block(block);
		context.train(block.train());		
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(block)) json.put(BLOCK, block.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		Id blockId = Id.from(json,BLOCK);
		if (isSet(blockId)) block = Block.get(blockId);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select block"),Block.selector(block, null));
		return super.properties(preForm, formInputs, postForm);
	}
	
	public String toString() {
		return isSet(block) ? t("Determine, which train is in {}",block) : t("[Click here to select block!]");
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id blockId = Id.from(params,Block.class.getSimpleName());
		if (isSet(blockId)) block = Block.get(blockId);
		return properties();
	}
}
