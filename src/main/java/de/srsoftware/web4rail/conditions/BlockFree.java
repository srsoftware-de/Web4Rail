package de.srsoftware.web4rail.conditions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
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
		if (isNull(context)) return false;
		if (isSet(context.train())) context = context.clone().train(null); // block.isFree would return true, if context.train == block.train. That has to be avoided!
		if (!inverted) return block.isFreeFor(context);
		return !block.isFreeFor(new Context(block)); // block.isFreeFor würde true liefern, wenn der Zug im Kontext gleich dem Zug im Block wäre. Da wir aber nur wissen wollen, ob der Block belegt ist, brauchen wir einen Context ohne Zug.
	}
	
	@Override
	public JSONObject json() {
		return super.json().put(BLOCK, block.id());
	}
	
	public Condition load(JSONObject json) {
		if (json.has(BLOCK)) new LoadCallback() {
			@Override
			public void afterLoad() {
				block(BaseClass.get(Id.from(json, BLOCK)));
			}
		};
		return super.load(json);
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

	@Override
	public String toString() {
		if (block == null) return "["+t("Click here to select block!")+"]";
		return t(inverted ? "Block {} is occupied":"Block {} is free",block);
	}


	@Override
	protected Object update(Params params) {
		if (params.containsKey(BLOCK)) {
			Tile tile = plan.get(new Id(params.getString(BLOCK)), true);
			if (tile instanceof Block) {
				block = (Block) tile;
			} else return t("Clicked tile is not a {}!",t("block"));
			return context().properties();
		}
		
		return super.update(params);
	}
}
