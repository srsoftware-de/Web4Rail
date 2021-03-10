package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.DelayedExecution;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class BlockFree extends Condition {
	
	private static final String BLOCK = Block.class.getSimpleName();
	private Block block;
	
	private BlockFree block(Block block) {
		this.block = block;
		return this;
	}

	@Override
	public boolean fulfilledBy(Context context) {
		return block.canNeEnteredBy(null) != inverted;
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
				new DelayedExecution(this) {					
					@Override
					public void execute() {
						block(BaseClass.get(bid));
					}
				};
			}
		}
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Block")+": "+(isNull(block) ? t("unset") : block),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,BLOCK)));
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
		Tile tile = plan.get(new Id(params.get(BLOCK)), true);
		if (tile instanceof Block) {
			block = (Block) tile;
		} else {
			return t("Clicked tile is not a {}!",t("block"));
		}
		
		return super.update(params);
	}
}
