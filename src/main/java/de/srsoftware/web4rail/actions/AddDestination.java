package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.DelayedExecution;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Tile;

public class AddDestination extends Action {

	private static final String TURN = "turn";
	private static final String SHUNTING = "shunting";
	private Block destination;
	private boolean turnAtDestination;
	private boolean shunting;
	
	public AddDestination(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context,Object cause) {
		Train train = context.train();		
		if (isNull(train)) return false;
		if (isNull(destination)) { // clear destinations!
			Iterator<String> it = train.tags().iterator();
			while (it.hasNext()) {
				String tag = it.next();
				if (tag.startsWith("@")) it = train.removeTag(tag);
			}
			return true;
		} 
		String flags = "+";
		if (turnAtDestination) flags += Train.TURN_FLAG;
		if (shunting) flags += Train.SHUNTING_FLAG;
		String dest = Train.DESTINATION_PREFIX+destination.id() + (flags.length()>1 ? flags : "");
		for (String tag: train.tags()) {
			if (tag.startsWith(Train.DESTINATION_PREFIX)) {
				train.removeTag(tag);
				dest = tag+dest;				
				break;
			}
		}		
		train.addTag(dest);
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(destination)) json.put(Train.DESTINATION,destination.id().toString());
		if (turnAtDestination) json.put(TURN,true);
		if (shunting) json.put(SHUNTING, true);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(TURN)) turnAtDestination = json.getBoolean(TURN);
		if (json.has(SHUNTING)) shunting = json.getBoolean(SHUNTING);
		if (json.has(Train.DESTINATION)) {
			Id blockId = new Id(json.getString(Train.DESTINATION));
			destination = BaseClass.get(blockId);
			if (isNull(destination)) {
				new DelayedExecution(this) {
					
					@Override
					public void execute() {
						destination = BaseClass.get(blockId);
					}
				};
			}
		}
		return super.load(json);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag span = new Tag("span");
		button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,Train.DESTINATION)).addTo(span);
		button(t("Clear destinations"),Map.of(ACTION,ACTION_UPDATE,Train.DESTINATION,"0")).addTo(span);
		formInputs.add(t("Destination")+": "+(isNull(destination) ? t("Clear destinations") : destination),span);
		formInputs.add(t("Turn at destination"),new Checkbox(TURN, t("Turn"), turnAtDestination));
		formInputs.add(t("Shunting"),new Checkbox(SHUNTING, t("Shunting"), shunting));
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return isSet(destination) ? t("Add {} to destinations of train",destination) : t("Clear destinations of train");
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(Train.DESTINATION)) {
			String destId = params.get(Train.DESTINATION);
			if ("0".equals(destId)) {
				destination = null;
			} else {
				Tile tile = plan.get(new Id(destId), true);
				if (tile instanceof Block) {
					destination = (Block) tile;
				} else {
					return t("Clicked tile is not a {}!",t("block"));
				}
			}
		}
		turnAtDestination = "on".equals(params.get(TURN));
		shunting = "on".equals(params.get(SHUNTING));
		return context().properties();
	}
}
