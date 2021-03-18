package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public class Switch extends Tile{
	
	private boolean state = false;
	private ActionList actionsOn,actionsOff;
	private static final String ON = "ON";
	private static final String OFF = "OFF";
	
	public Switch() {
		actionsOn = new ActionList(this);
		actionsOff = new ActionList(this);
	}
	
	@Override
	protected HashSet<String> classes() {
		HashSet<String> classes = super.classes();
		classes.add(state?"on":"off");
		return classes;
	}

	
	@Override
	public Object click(boolean shift) throws IOException {
		if (!shift) state(!state);
		return super.click(shift);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (!actionsOn.isEmpty()) json.put(ON, actionsOn.json());
		if (!actionsOff.isEmpty()) json.put(OFF, actionsOff.json());
		json.put(STATE, state);
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(ON)) {
			Object dummy = json.get(ON);
			if (dummy instanceof JSONArray) {
				JSONArray jarr = (JSONArray) dummy;
				for (Object o : jarr) {
					if (o instanceof JSONObject) {
						JSONObject jo = (JSONObject) o;
						String type = jo.getString("type");
						Action action = Action.create(type, actionsOn);
						if (isSet(action)) {
							action.load(jo);
							actionsOn.add(action);
						}
					}
				}
			}
			if (dummy instanceof JSONObject) {
				actionsOn.load((JSONObject) dummy);
			}
		}
		if (json.has(OFF)) {
			Object dummy = json.get(OFF);
			if (dummy instanceof JSONArray) {
				JSONArray jarr = (JSONArray) dummy;
				for (Object o : jarr) {
					if (o instanceof JSONObject) {
						JSONObject jo = (JSONObject) o;
						String type = jo.getString("type");
						Action action = Action.create(type, actionsOff);
						if (isSet(action)) {
							action.load(jo);
							actionsOff.add(action);
						}
					}
				}
			}
			if (dummy instanceof JSONObject) {
				actionsOff.load((JSONObject) dummy);
			}
		}
		if (json.has(STATE)) state =json.getBoolean(STATE);
		return super.load(json);
	}
	
	@Override
	public Tile position(int x, int y) {
		super.position(x, y);
		return this;
	}
	
	public static Object process(HashMap<String, String> params) {
		String action = params.get(ACTION);
		Id id = Id.from(params);
		if (action == null) return t("Missing ACTION on call to {}.process()",Switch.class.getSimpleName());
		Switch swtch = isSet(id) ? BaseClass.get(id) : null;
		switch (action) {
			case ACTION_DROP:
				if (isNull(id)) return t("Missing ID on call to {}.process()",Switch.class.getSimpleName());
				if (isNull(swtch)) return t("No contact with id {} found!",id);
				swtch.remove();
				return t("Removed {}.",id);
			case ACTION_PROPS:
				return swtch.properties();
			case ACTION_UPDATE:
				return plan.update(params);
		}
		return t("Unknown action: {}",action);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Fieldset fieldset = new Fieldset(t("Actions (On)"));
		fieldset.id("actionsOn");
		actionsOn.list().addTo(fieldset);
		postForm.add(fieldset);
		fieldset = new Fieldset(t("Actions (Off)"));		
		fieldset.id("actionsOff");
		actionsOff.list().addTo(fieldset);
		postForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public void removeChild(BaseClass child) {
		if (child == actionsOn) actionsOn = null;
		if (child == actionsOff) actionsOff = null;
		super.removeChild(child);
	}
	
	public static Select selector(Switch preselect) {
		TreeMap<String,Switch> sortedSet = new TreeMap<String, Switch>(); // Map from Name to Contact
		for (Switch contact : BaseClass.listElements(Switch.class)) sortedSet.put(contact.toString(), contact);
		Select select = new Select("Switch");
		for (Entry<String, Switch> entry : sortedSet.entrySet()) {
			Switch contact = entry.getValue();
			Tag option = select.addOption(contact.id(),contact);
			if (contact == preselect) option.attr("selected", "selected");
		}
		return select;
	}
	
	public boolean isOn() {
		return state;
	}
	
	public void state(boolean newState) {
		state = newState;

		new Thread(Application.threadName(this)) {
			
			@Override
			public void run() {
				Context context = new Context(Switch.this);
				if (state) {
					actionsOn.fire(context,Switch.this);
				} else actionsOff.fire(context,Switch.this);
			}
		}.start();
		stream();
	}
	
	public void stream() {
		try {			
			Tag tag = tag(null);
			if (state) tag.clazz(tag.get("class")+" active");
			plan.stream("place "+tag);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String title() {
		return t("Switch @ ({}, {})",x,y);
	}
	
	@Override
	public String toString() {
		return t("Switch")+"("+x+","+y+")";
	}
}
