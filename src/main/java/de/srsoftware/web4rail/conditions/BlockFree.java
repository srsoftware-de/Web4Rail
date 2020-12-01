package de.srsoftware.web4rail.conditions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tiles.Block;

public class BlockFree extends Condition {
	
	private static final String BLOCK = Block.class.getSimpleName();
	private Block block;

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
		block(Block.get(new Id(json.getString(BLOCK))));
		return this;
	}
	
	@Override
	public Tag propForm(HashMap<String, String> params) {
		Tag form = super.propForm(params);
		Block.selector(block, null).addTo(new Label(t("Select block:")+NBSP)).addTo(form);
		return form;
	}

	@Override
	public String toString() {
		if (block == null) return t("[Click here to select block!]");
		return t(inverted ? "Block {} is occupied":"Block {} is free",block);
	}
	
	private BlockFree block(Block block) {
		this.block = block;
		return this;
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(BLOCK)) return t("No block id passed to BlockFree.update()!");
		Id bid = new Id(params.get(BLOCK));
		Block block = Block.get(bid);
		if (block == null) return t("No block with id {} found!",bid);
		this.block = block;
		return super.update(params);
	}
}
