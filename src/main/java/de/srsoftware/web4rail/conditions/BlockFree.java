package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tiles.Block;

public class BlockFree extends Condition {
	
	private static final String BLOCK = Block.class.getSimpleName();
	private Block block;
	
	private BlockFree block(Block block) {
		this.block = block;
		return this;
	}

	@Override
	public boolean fulfilledBy(Context context) {
		return block.isFreeFor(null) != inverted;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(BLOCK, block.id());
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(BLOCK)) {
			Id bid = new Id(json.getString(BLOCK));
			block(BaseClass.get(bid));
			if (isNull(block)) {
				Application.threadPool.execute(new Thread() {
					@Override
					public void run() {
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						block(BaseClass.get(bid));
					}
				});
			}
		}
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Select block"), Block.selector(block, null));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == block) block = null;
		super.removeChild(child);
	}	

	@Override
	public String toString() {
		if (block == null) return "["+t("Click here to select block!")+"]";
		return t(inverted ? "Block {} is occupied":"Block {} is free",block);
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(BLOCK)) return t("No block id passed to BlockFree.update()!");
		Id bid = new Id(params.get(BLOCK));
		Block block = Block.get(bid);
		if (isNull(block)) return t("No block with id {} found!",bid);
		this.block = block;
		return super.update(params);
	}
}
