package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Tile;

public class DisableEnableTile extends Action {
		
	public DisableEnableTile(BaseClass parent) {
		super(parent);
	}

	private Tile tile = null;
	private boolean disable = true;
	
	@Override
	public boolean fire(Context context) {
		if (isNull(tile)) tile = context.block();
		if (isNull(tile)) return false;
		tile.setEnabled(!disable);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(tile)) json.put(BLOCK, tile.id());
		json.put(STATE, !disable);
		return json;
	}
	
	@Override
	protected String highlightId() {
		return isSet(tile) ? tile.id().toString() : null;
	}
	
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(STATE)) disable = !json.getBoolean(STATE);
		if (json.has(BLOCK)) new LoadCallback() {
			@Override
			public void afterLoad() {
				tile = Block.get(Id.from(json,BLOCK));
			}
		};						
		return super.load(json);

	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Block")+": "+(isNull(tile) ? t("tile from context") : tile),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,Tile.class.getSimpleName())));
		Tag radios = new Tag("p");
		new Radio(STATE, "enable", t("enable"), !disable).addTo(radios);
		new Radio(STATE, "disable", t("disable"), disable).addTo(radios);
		formInputs.add(t("Action"),radios);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == tile) tile = null;
		super.removeChild(child);
	}
	
	public String toString() {
		String blk = isSet(tile) ? tile.toString() : t("block from context");
		return t(disable ? "disable {}" : "enable {}",blk);
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		Id tileId = Id.from(params,Tile.class.getSimpleName());
		Tile newTile = BaseClass.get(tileId);;
		if (newTile instanceof Shadow) newTile = ((Shadow)newTile).overlay();
		if (isSet(newTile)) tile = newTile;
		disable = !"enable".equals(params.get(STATE)); 
		return super.update(params);
	}
}
