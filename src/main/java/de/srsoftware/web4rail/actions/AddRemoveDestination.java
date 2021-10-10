package de.srsoftware.web4rail.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.Tile;

public class AddRemoveDestination extends Action {

	private static final String TURN = "turn";
	private static final String SHUNTING = "shunting";
	private static final String TRIGGER = "destination_trigger";
	private Block destination;
	private boolean turnAtDestination;
	private boolean shunting;
	private Tile destinationTrigger = null;
	
	public AddRemoveDestination(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
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
		train.setDestinationTrigger(destinationTrigger);
		return true;
	}
	
	@Override
	protected String highlightId() {
		return isSet(destination) ? destination.id().toString() : null;
	}
	
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(destination)) json.put(Train.DESTINATION,destination.id().toString());
		if (turnAtDestination) json.put(TURN,true);
		if (shunting) json.put(SHUNTING, true);
		if (isSet(destinationTrigger)) json.put(TRIGGER, destinationTrigger.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(TURN)) turnAtDestination = json.getBoolean(TURN);
		if (json.has(SHUNTING)) shunting = json.getBoolean(SHUNTING);
		if (json.has(Train.DESTINATION)) new LoadCallback() {
			@Override
			public void afterLoad() {
				destination = BaseClass.get(Id.from(json, Train.DESTINATION));
			}
		};
		if (json.has(TRIGGER)) new LoadCallback() {
			
			@Override
			public void afterLoad() {
				destinationTrigger = Tile.get(Id.from(json, TRIGGER));
			}
		};
		return super.load(json);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Tag span = new Tag("span");
		button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,Train.DESTINATION)).addTo(span);
		button(t("Clear destinations"),Map.of(ACTION,ACTION_UPDATE,Train.DESTINATION,"0")).addTo(span);
		formInputs.add(t("Destination")+": "+(isNull(destination) ? t("Clear destinations") : destination),span);
		formInputs.add(t("Turn at destination"),new Checkbox(TURN, t("Turn"), turnAtDestination));
		formInputs.add(t("Shunting"),new Checkbox(SHUNTING, t("Shunting"), shunting));
		formInputs.add(t("Trigger Contact/Switch at destination")+": "+(isNull(destinationTrigger) ? t("unset") : destinationTrigger),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,CONTACT)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isNull(destination)) return t("Clear destinations of train");
		String suffix = turnAtDestination ? t("Turn") : null;
		if (shunting) suffix = (isSet(suffix) ? suffix+" + " : "")+t("Shunting");
		return t("Add {} to destinations of train",destination)+(isSet(suffix) ? " ("+suffix+")" : "");
	}
	
	@Override
	protected Object update(Params params) {
		if (params.containsKey(Train.DESTINATION)) {
			String destId = params.getString(Train.DESTINATION);
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
		if (params.containsKey(CONTACT)) {
			Tile tile = Tile.get(Id.from(params,CONTACT));
			if (tile instanceof Contact || tile instanceof Switch) destinationTrigger = tile;
		}
		turnAtDestination = "on".equals(params.getString(TURN));
		shunting = "on".equals(params.getString(SHUNTING));
		return context().properties();
	}
}
