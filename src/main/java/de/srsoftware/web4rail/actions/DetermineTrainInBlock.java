package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Block;

public class DetermineTrainInBlock extends Action {
		
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
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		
		Select select = Block.selector(block, null);
		select.addTo(new Label(t("Select block:")+NBSP)).addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	public String toString() {
		return isSet(block) ? t("Determine, which train is in {}",block) : t("[Click here to select block!]");
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id blockId = Id.from(params,Block.class.getSimpleName());
		if (isSet(blockId)) block = Block.get(blockId);
		return properties(params);
	}
}
