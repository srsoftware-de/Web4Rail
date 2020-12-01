package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public class Contact extends Tile{
	
	private static final String ADDRESS = "address";
	private static final HashMap<Id, Contact> contactsById = new HashMap<Id, Contact>();
	private static final HashMap<Integer, Contact> contactsByAddr = new HashMap<Integer, Contact>();
	private boolean state = false;
	private String trigger = null;
	private int addr = 0;
	private ActionList actions = new ActionList();
	private OffTimer timer = null;
	
	/**
	 * Dieser Timer dient dazu, Merhfachauslösungen eines Kontakes innerhalb einer Sekunde zu unterbinden
	 *
	 */
	private class OffTimer extends Thread {
		
		boolean aborted = false;
		
		public OffTimer() {
			start();
		}
		
		@Override
		public void run() {
			try {
				for (int ticks = 0; ticks<10; ticks++) {
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
			if (isSet(route)) {
				route.contact(this);
			} else if (getClass() != Contact.class) {
				plan.warn(this);	
			}
			actions.fire(new Context(this));
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
	
	public static Contact get(Id contactId) {
		return contactsById.get(contactId);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (addr > 0) json.put(ADDRESS, addr);
		if (!actions.isEmpty()) json.put(REALM_ACTIONS, actions.json());
		return json;
	}
	
	public static Collection<Contact> list() {
		return contactsById.values();
	}

	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		if (json.has(ADDRESS)) addr(json.getInt(ADDRESS));
		if (json.has(REALM_ACTIONS)) actions = ActionList.load(json.getJSONArray(REALM_ACTIONS));
		return this;
	}
	
	@Override
	public Tile position(int x, int y) {
		super.position(x, y);
		contactsById.put(id(), this);
		return this;
	}
	
	public static Object process(HashMap<String, String> params) {
		String action = params.get(ACTION);
		Id id = Id.from(params);
		if (action == null) return t("Missing ACTION on call to {}.process()",Contact.class.getSimpleName());
		Contact contact;
		switch (action) {
			case ACTION_ANALYZE:
				if (id == null) return t("Missing ID on call to {}.process()",Contact.class.getSimpleName());
				contact = contactsById.get(id);
				if (contact == null) return t("No contact with id {} found!",id);
				Tag propMenu = contact.propMenu();
				propMenu.children().insertElementAt(new Tag("div").content(t("Trigger a feedback sensor to assign it with this contact!")), 1);
				plan.learn(contact);
				return propMenu;
		}
		return t("Unknown action: {}",action);
	}

	
	@Override
	public Form propForm(String formId) {
		Form form = super.propForm(formId);
		new Tag("h4").content(t("Hardware settings")).addTo(form);
		Tag label = new Input(ADDRESS, addr).numeric().addTo(new Label(t("Address:")+NBSP));
		button(t("learn"),Map.of(ACTION,ACTION_ANALYZE)).addTo(label).addTo(form);		
		return form;
	}
	
	@Override
	public Window propMenu() {
		Window win = super.propMenu();
		new Tag("h4").content(t("Actions")).addTo(win);
		actions.addTo(win, REALM_PLAN+":"+id());
		return win;
	}
	
	public static Select selector(Contact preselect) {
		TreeMap<String,Contact> sortedSet = new TreeMap<String, Contact>(); // Map from Name to Contact
		for (Contact contact : contactsById.values()) sortedSet.put(contact.toString(), contact);
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
			Tag tag = super.tag(null);
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
		new Thread() {
			public void run() {
				try {
					sleep(duration);
					activate(false);
				} catch (Exception e) {}
			}
		}.start();
		return true;
	}
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(ADDRESS)) addr(Integer.parseInt(params.get(ADDRESS)));
		return super.update(params);
	}
}
