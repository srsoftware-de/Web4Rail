package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class DetermineTrainInBlock extends Action {
		
	public DetermineTrainInBlock(BaseClass parent) {
		super(parent);
	}

	private Block block = null;
	
	@Override
	public boolean fire(Context context,Object cause) {
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
		if (isSet(blockId)) new LoadCallback() {
			
			@Override
			public void afterLoad() {
				block = Block.get(blockId);
			}
		};
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Block")+": "+(isNull(block) ? t("unset") : block),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,BLOCK)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == block) block = null;
		super.removeChild(child);
	}
	
	public String toString() {
		return isSet(block) ? t("Determine, which train is in {}",block) : "["+t("Click here to select block!")+"]";
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(BLOCK)) {
			Tile tile = plan.get(new Id(params.get(BLOCK)), true);
			if (tile instanceof Block) {
				block = (Block) tile;
			} else return t("Clicked tile is not a {}!",t("block"));
		}
		return context().properties();
	}
}
