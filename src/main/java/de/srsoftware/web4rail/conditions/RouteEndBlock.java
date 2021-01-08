package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tiles.Block;

public class RouteEndBlock extends Condition{
	
	private static final String BLOCK = Block.class.getSimpleName();
	private Block block;
	
	private RouteEndBlock block(Block block) {
		this.block = block;
		return this;
	}

	@Override
	public boolean fulfilledBy(Context context) {
		if (isNull(context)) return false;
		Route route = context.route();
		if (isNull(route)) return false;
		return route.endBlock() == block;
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
		return t(inverted ? "Route does not end at {}.":"Route ends at {}.",block);
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(BLOCK)) return t("No block id passed to RouteEndBlock.update()!");
		Id bid = new Id(params.get(BLOCK));
		Block block = Block.get(bid);
		if (block == null) return t("No block with id {} found!",bid);
		this.block = block;
		return super.update(params);
	}
}
