package de.srsoftware.web4rail.conditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Tile;

public class TrainWasInBlock extends Condition {
	
	private static final String BLOCK = Block.class.getSimpleName();
	private static final String COUNT = "count";
	private Block block;
	private int count = 5;
	
	private TrainWasInBlock block(Block block) {
		this.block = block;
		return this;
	}

	@Override
	public boolean fulfilledBy(Context context) {
		Train train = context.train();
		if (isNull(train)) return false;		
		return train.lastBlocks(count).contains(block) != inverted;
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(BLOCK, block.id()).put(COUNT, count);
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(BLOCK)) block(Block.get(new Id(json.getString(BLOCK))));
		if (json.has(COUNT)) count = json.getInt(COUNT);
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Block")+": "+(isNull(block) ? t("block from context") : block),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,BLOCK)));
		formInputs.add(t("Seek in last"), new Input(COUNT, count).numeric().addTo(new Tag("span")).content(NBSP+t("blocks of train")));
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
		return t(inverted ? "{} not within last {} blocks of train":"{} within last {} blocks of train",block,count);
	}


	@Override
	protected Object update(HashMap<String, String> params) {
		if (!params.containsKey(BLOCK)) return t("No block id passed to TrainWasInBlock.update()!");
		Id bid = new Id(params.get(BLOCK));
		Tile tile = BaseClass.get(bid);
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay();
		if (tile instanceof Block) block = (Block) tile;
		if (params.containsKey(COUNT)) count=Integer.parseInt(params.get(COUNT));
		return super.update(params);
	}
}
