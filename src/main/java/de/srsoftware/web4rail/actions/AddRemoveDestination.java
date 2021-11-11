package de.srsoftware.web4rail.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Destination;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.Tile;

public class AddRemoveDestination extends Action {

	private static final String TRIGGER = "destination_trigger";
	private static final String TURN = "turn";
	private static final String SHUNTING = "shunting";
	private static final String FROM = "from";
	private Destination destination;
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
		String dest = destination.tag();
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
		return isSet(destination) ? destination.block() : null;
	}
	
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(destination)) {
			json.put(Train.DESTINATION,destination.block());
			if (destination.turn()) json.put(TURN,true);
			if (destination.shunting()) json.put(SHUNTING, true);
			if (isSet(destination.enterFrom())) json.put(FROM, destination.enterFrom());
		}
		if (isSet(destinationTrigger)) json.put(TRIGGER, destinationTrigger.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(Train.DESTINATION)) new LoadCallback() {
			@Override
			public void afterLoad() {
				Id id = Id.from(json, Train.DESTINATION);
				Block block = Block.get(id);
				if (isNull(block)) {
					LOG.warn("Unknown block id \"{}\" encountered during AddRemoveDestination.load(json)",id);
					return;
				}
				destination = new Destination(block);
				
				if (json.has(TURN)) destination.turn(json.getBoolean(TURN));
				if (json.has(SHUNTING)) destination.shunting(json.getBoolean(SHUNTING));
				if (json.has(FROM)) destination.enterFrom(Direction.valueOf(json.getString(FROM)));
				
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
		if (isSet(destination)) {
			formInputs.add(t("Turn at destination"),new Checkbox(TURN, t("Turn"), destination.turn()));
			formInputs.add(t("Shunting"),new Checkbox(SHUNTING, t("Shunting"), destination.shunting()));
		}
		formInputs.add(t("Trigger Contact/Switch at destination")+": "+(isNull(destinationTrigger) ? t("unset") : destinationTrigger),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,CONTACT)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isNull(destination)) return t("Clear destinations of train");
		String suffix = destination.turn() ? t("Turn") : null;
		if (destination.shunting()) suffix = (isSet(suffix) ? suffix+" + " : "")+t("Shunting");
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
					Block block = (Block) tile;
					destination = new Destination(block,block.enterDirection(destId));
				} else {
					return t("Clicked tile is not a {}!",t("block"));
				}
			}
		}
		if (params.containsKey(CONTACT)) {
			Tile tile = Tile.get(Id.from(params,CONTACT));
			if (tile instanceof Contact || tile instanceof Switch) destinationTrigger = tile;
		}
		if (isSet(destination)) {
			destination.turn("on".equals(params.getString(TURN)));
			destination.shunting("on".equals(params.getString(SHUNTING)));
		}
		return context().properties();
	}
}
