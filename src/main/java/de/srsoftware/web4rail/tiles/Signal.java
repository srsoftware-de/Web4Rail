package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Table;

public abstract class Signal extends Tile {
	public static final String STATE = "state";
	public static final String RED = "red";
	public static final String GREEN = "green";
	public static final TreeSet<String> knownStates = new TreeSet<String>(List.of(RED, GREEN));
	private final HashMap<String,HashSet<int[]>> aspects = new HashMap<String, HashSet<int[]>>();
	private final HashSet<Integer> initialized = new HashSet<Integer>();
	private static final String ADDRESS = "addr";
	private static final String HOLD = "hold";
	private static final String NEW_ASPECT = "new_aspect";
	private static final String ASPECTS = "aspects";
	private   Protocol protocol    = Protocol.DCC128;
	
	private String state = RED;

	public Signal() {
		super();
	}
	
	@Override
	protected Vector<String> classes() {
		Vector<String> classes = super.classes();
		classes.add("signal");
		return classes;
	}
	
	public abstract boolean isAffectedFrom(Direction dir);
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (!aspects.isEmpty()) {
			JSONObject jAspects = new JSONObject();
			for (Entry<String, HashSet<int[]>> entry : aspects.entrySet()) {
				String aspect = entry.getKey();
				HashSet<int[]> commands = entry.getValue();
				if (isSet(commands)) {
					JSONArray jCommands = new JSONArray();
					for (int[] data : commands) {
						JSONObject jData = new JSONObject();
						jData.put(ADDRESS, data[0]);
						jData.put(PORT, data[1]);
						jData.put(STATE, data[2]);
						jData.put(HOLD, data[3]>0);
						jCommands.put(jData);
					}
					jAspects.put(aspect, jCommands);
				}
			}
			json.put(ASPECTS, jAspects);
		}
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(ASPECTS)) {
			JSONObject jAspects = json.getJSONObject(ASPECTS);
			for (String aspect : jAspects.keySet()) {
				knownStates.add(aspect);
				JSONArray jCommands = jAspects.getJSONArray(aspect);
				jCommands.forEach(o -> {
					if (o instanceof JSONObject) {
						JSONObject d = (JSONObject) o;
						int[] data = new int[] {d.getInt(ADDRESS),d.getInt(PORT),d.getInt(STATE),d.getBoolean(HOLD)?1:0};
						HashSet<int[]> commands = aspects.get(aspect);
						if (isNull(commands)) {
							commands = new HashSet<int[]>();
							aspects.put(aspect, commands);
						}
						commands.add(data);
					}
				});
			}
		}
		return super.load(json);
	}
	
	private char proto() {
		switch (protocol) {
		case DCC14:
		case DCC27:
		case DCC28:
		case DCC128:
			return 'N';
		case MOTO:
			return 'M';
		case SELECTRIX:
			return 'S';
		default:
			return 'P';
		}		
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Fieldset aspectEditor = new Fieldset(t("Aspects"));
		aspectEditor.attr(ID, "aspect-editor");
		Form form = new Form("aspect-form");
		new Input(REALM,REALM_PLAN).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(ID,id()).hideIn(form);
		Table table = new Table();
		table.addHead(t("Aspect"),t("Address"),t("Port"),t("State"),t("Hold"),t("Actions"));
		
		for (String aspect : knownStates) {
			HashSet<int[]> commands = aspects.get(aspect);
			if (isSet(commands)) {
				Tag link = this.link("span", (Object)aspect, Map.of(ACTION,ACTION_POWER,STATE,aspect));
				for (int[] command : aspects.get(aspect)) {
					Button delete = this.button(t("delete"), Map.of(ACTION,ACTION_UPDATE,ACTION_DROP+"-"+aspect,command[0]+"-"+command[1]+"-"+command[2]));
					table.addRow(link,command[0],command[1],command[2],command[3]==1?"✓":"",delete);
				}
			}
		}
		for (String aspect : knownStates) {
			table.addRow(t("add command for {}",aspect),new Input(ADDRESS+"-"+aspect, 0).numeric(),new Input(PORT+"-"+aspect,0).numeric(),new Input(STATE+"-"+aspect,0).numeric(),new Checkbox(HOLD+"-"+aspect, "", true));
		}
		
		Tag buttons = new Tag("div");
		new Button(t("Save"), form).addTo(buttons);
		table.addRow(t("add new aspect"),new Input(NEW_ASPECT),"","","",buttons);
		
		table.addTo(form);
		form.addTo(aspectEditor);
		
		postForm.add(aspectEditor);
		return super.properties(preForm, formInputs, postForm);
	}

	public boolean state(String aspect) {
		LOG.debug("{}.state({})",this,aspect);
		this.state = aspect;
		HashSet<int[]> commands = aspects.get(aspect);
		if (isSet(commands)) {
			for (int[] data : commands) {
				int addr = data[0];
				init(addr);
				Command cmd = new Command("SET {} GA "+data[0]+" "+data[1]+" "+data[2]+" "+(data[3]==1?-1:200));
				LOG.debug("new Command: {}",cmd);
				plan.controlUnit().queue(cmd);
			}
		}
		plan.place(this);
		return true;
	}
	
	private void init(int addr) {
		if (!initialized.contains(addr)) {
			Command command = new Command("INIT {} GA "+addr+" "+proto()) {

				@Override
				public void onSuccess() {
					super.onSuccess();
					initialized.add(addr);
				}

				@Override
				public void onFailure(Reply r) {
					super.onFailure(r);
				}
				
			};			
			try {
				plan.queue(command).reply();
			} catch (TimeoutException e) {
				LOG.warn(e.getMessage());
			}

		}
	}

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+" "+state);
		return tag;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		HashMap<String,int[]> newAspects = new HashMap<String, int[]>();
		for (Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue().trim();
			
			String[] parts = key.split("-");
			if (parts.length>1) {
				String subject = parts[0];
				String aspect  = parts[1];
				
				int[] data = newAspects.get(aspect);
				if (isNull(data)) {
					data = new int[] {0,0,0,0};
					newAspects.put(aspect,data);
				}
				switch (subject) {
					case ADDRESS:
						data[0] = Integer.parseInt(val); break;
					case ACTION_DROP:
						parts = val.split("-");
						HashSet<int[]> commands = aspects.get(aspect);
						if (isSet(commands)) for (int[] d : commands) {								
							if (d[0] != Integer.parseInt(parts[0])) continue;
							if (d[1] != Integer.parseInt(parts[1])) continue;
							if (d[2] != Integer.parseInt(parts[2])) continue;
							commands.remove(d);
							break;
						}
						break;
					case HOLD:
						data[3] = "on".equalsIgnoreCase(val)?1:0; break;
					case PORT:
						data[1] = Integer.parseInt(val); break;
					case STATE:
						data[2] = Integer.parseInt(val); break;
				}
			} else switch (key) {
				case NEW_ASPECT:
					if (!val.isEmpty()) knownStates.add(val); break;
			}
		}
		for (Entry<String, int[]> entry : newAspects.entrySet()) {
			String aspect = entry.getKey();
			int[] data = entry.getValue();
			if (data[0]==0) continue;
			LOG.debug("{} : {} / {} / {}",entry.getKey(),data[0],data[1],data[2]);
			HashSet<int[]> dataSet = aspects.get(aspect);
			if (isNull(dataSet)) {
				dataSet = new HashSet<int[]>();
				aspects.put(aspect, dataSet);
			}
			aspects.get(aspect).add(data);
		}
		return super.update(params);
	}
}
