package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Tile;

public class DisableEnableBlock extends Action {
		
	public DisableEnableBlock(BaseClass parent) {
		super(parent);
	}

	private Block block = null;
	private boolean disable = true;
	
	@Override
	public boolean fire(Context context,Object cause) {
		if (isNull(block)) block = context.block();
		if (isNull(block)) return false;
		block.setEnabled(!disable);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(block)) json.put(BLOCK, block.id());
		json.put(STATE, !disable);
		return json;
	}
	
	@Override
	protected String highlightId() {
		return isSet(block) ? block.id().toString() : null;
	}
	
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(STATE)) disable = !json.getBoolean(STATE);
		if (json.has(BLOCK)) new LoadCallback() {
			@Override
			public void afterLoad() {
				block = Block.get(Id.from(json,BLOCK));
			}
		};						
		return super.load(json);

	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Block")+": "+(isNull(block) ? t("block from context") : block),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,Block.class.getSimpleName())));
		Tag radios = new Tag("p");
		new Radio(STATE, "enable", t("enable"), !disable).addTo(radios);
		new Radio(STATE, "disable", t("disable"), disable).addTo(radios);
		formInputs.add(t("Action"),radios);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == block) block = null;
		super.removeChild(child);
	}
	
	public String toString() {
		String blk = isSet(block) ? block.toString() : t("block from context");
		return t(disable ? "disable {}" : "enable {}",blk);
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id blockId = Id.from(params,Block.class.getSimpleName());
		Tile tile = isSet(blockId) ? BaseClass.get(blockId) : null;
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		if (tile instanceof Block) block = (Block) tile;
		disable = !"enable".equals(params.get(STATE)); 
		return properties();
	}
}
