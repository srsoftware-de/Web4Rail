package de.srsoftware.web4rail.conditions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

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
		new LoadCallback() {
			
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
		return t(inverted ? "Route does not end at {}.":"Route ends at {}.",block);
	}


	@Override
	protected Object update(Params params) {
		if (params.containsKey(BLOCK)) {
			Tile tile = plan.get(new Id(params.getString(BLOCK)), true);
			if (tile instanceof Block) {
				block = (Block) tile;
			} else return t("Clicked tile is not a {}!",t("block"));
		}
		return super.update(params);
	}
}
