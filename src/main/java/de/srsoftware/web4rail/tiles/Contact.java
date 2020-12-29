package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;

public class Contact extends Tile{
	
	private static final String ADDRESS = "address";
	private static final HashMap<Integer, Contact> contactsByAddr = new HashMap<Integer, Contact>();
	private boolean state = false;
	private String trigger = null;
	protected int addr = 0;
	private ActionList actions;
	private OffTimer timer = null;
	
	public Contact() {
		actions = new ActionList(this);
	}
	
	/**
	 * Dieser Timer dient dazu, Merhfachauslösungen eines Kontakes innerhalb einer Sekunde zu unterbinden
	 *
	 */
	private class OffTimer extends Thread {
		
		boolean aborted = false;
		
		public OffTimer() {
			Application.threadPool.execute(this);
		}
		
		@Override
		public void run() {
			try {
				for (int ticks = 0; ticks<50; ticks++) {
					if (!aborted) sleep(10);
				}
				timer = null;
				if (aborted) return;
				state = false;
				stream();
			} catch (InterruptedException e) {}
		}

		private void abort() {
			aborted = true;
		}
	}
	
	public void activate(boolean newState) {
		if (newState == state) return;
		
		if (newState == false) {
			if (isSet(timer)) return;
			timer = new OffTimer();
		} else {
			LOG.debug("{} activated.",this);
			state = true;
			if (isSet(timer)) timer.abort();
			Context context = null;
			Route route = route();
			if (isSet(route)) {
				context = route.context();
				actions.fire(context);
				route.contact(this);
			}
			if (isNull(context)) {
				context = new Context(this);
				actions.fire(context);
			}
			
			stream();
		}
	}
	
	public int addr() {
		return addr;
	}
	
	public Contact addr(int address) {
		contactsByAddr.remove(addr); // alte ID aus der Map löschen
		addr = address;
		if (addr != 0) contactsByAddr.put(addr, this); // neue ID setzen
		return this;
	}

	@Override
	public Object click() throws IOException {
		trigger(200);
		return super.click();
	}
	
	public static Contact get(int addr) {
		return contactsByAddr.get(addr);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (addr > 0) json.put(ADDRESS, addr);
		if (!actions.isEmpty()) json.put(REALM_ACTIONS, actions.json());
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(ADDRESS)) addr(json.getInt(ADDRESS));
		if (json.has(REALM_ACTIONS)) {
			Object dummy = json.get(REALM_ACTIONS);
			if (dummy instanceof JSONArray) {
				JSONArray jarr = (JSONArray) dummy;
				for (Object o : jarr) {
					if (o instanceof JSONObject) {
						JSONObject jo = (JSONObject) o;
						String type = jo.getString("type");
						Action action = Action.create(type, actions);
						if (isSet(action)) {
							action.load(jo);
							actions.add(action);
						}
					}
				}
			}
			if (dummy instanceof JSONObject) {
				actions.load((JSONObject) dummy);
			}
		}
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
		if (action == null) return t("Missing ACTION on call to {}.process()",Contact.class.getSimpleName());
		Contact contact = isSet(id) ? BaseClass.get(id) : null;
		switch (action) {
			case ACTION_ANALYZE:
				if (isNull(id)) return t("Missing ID on call to {}.process()",Contact.class.getSimpleName());
				if (isNull(contact)) return t("No contact with id {} found!",id);
				Tag propMenu = contact.properties();
				propMenu.children().insertElementAt(new Tag("div").content(t("Trigger a feedback sensor to assign it with this contact!")), 1);
				plan.learn(contact);
				return propMenu;
			case ACTION_DROP:
				if (isNull(id)) return t("Missing ID on call to {}.process()",Contact.class.getSimpleName());
				if (isNull(contact)) return t("No contact with id {} found!",id);
				contact.remove();
				if (contact instanceof BlockContact) return contact.properties();
				return t("Removed {}.",id);
			case ACTION_UPDATE:
				return plan.update(params);
		}
		return t("Unknown action: {}",action);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag span = new Tag("span");
		new Input(ADDRESS, addr).numeric().addTo(span).content(NBSP);
		button(t("learn"),Map.of(ACTION,ACTION_ANALYZE)).addTo(span);
		formInputs.add(t("Address"),span);
		
		Fieldset fieldset = new Fieldset(t("Actions"));
		actions.list().addTo(fieldset);
		postForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public void removeChild(BaseClass child) {
		if (child == actions) actions = null;
		super.removeChild(child);
	}
	
	public static Select selector(Contact preselect) {
		TreeMap<String,Contact> sortedSet = new TreeMap<String, Contact>(); // Map from Name to Contact
		for (Contact contact : BaseClass.listElements(Contact.class)) sortedSet.put(contact.toString(), contact);
		Select select = new Select(CONTACT);
		for (Entry<String, Contact> entry : sortedSet.entrySet()) {
			Contact contact = entry.getValue();
			Tag option = select.addOption(contact.id(),contact);
			if (contact == preselect) option.attr("selected", "selected");
		}
		return select;
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
		return t("Contact {} @ ({}, {})",addr,x,y);
	}

	public String trigger() {
		if (trigger == null) trigger = getClass().getSimpleName()+"-"+id();
		return trigger;
	}
	
	public boolean trigger(int duration) {
		activate(true);
		Application.threadPool.execute(new Thread() {
			public void run() {
				try {
					sleep(duration);
					activate(false);
				} catch (Exception e) {}
			}
		});
		return true;
	}
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(ADDRESS)) addr(Integer.parseInt(params.get(ADDRESS)));
		return super.update(params);
	}
}
