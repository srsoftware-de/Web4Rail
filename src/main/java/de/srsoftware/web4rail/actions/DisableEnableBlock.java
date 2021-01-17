package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;

public class DisableEnableBlock extends Action {
		
	public DisableEnableBlock(BaseClass parent) {
		super(parent);
	}

	private Block block = null;
	private boolean disable = true;
	
	@Override
	public boolean fire(Context context) {
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
	public Action load(JSONObject json) {
		super.load(json);
		Id blockId = Id.from(json,BLOCK);
		if (isSet(blockId)) {
			block = Block.get(blockId);
			if (isNull(block)) {
				Application.threadPool.execute(new Thread() {
					public void run() {
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						block = Block.get(blockId);
					};
				});
			}
		}
		if (json.has(STATE)) {
			disable = !json.getBoolean(STATE);
		}
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select block"),Block.selector(isSet(block) ? block : t("block from context"), null));
		Tag radios = new Tag("p");
		new Radio(STATE, "enable", t("enable"), !disable).addTo(radios);
		new Radio(STATE, "disable", t("disable"), disable).addTo(radios);
		formInputs.add(t("Action"),radios);
		return super.properties(preForm, formInputs, postForm);
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
		if (isSet(blockId)) block = Block.get(blockId);
		disable = !"enable".equals(params.get(STATE)); 
		return properties();
	}
}
