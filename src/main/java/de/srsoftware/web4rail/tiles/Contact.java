package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public abstract class Contact extends Tile{
	
	private static final String ADDRESS = "address";
	private static final HashMap<String, Contact> contactsById = new HashMap<String, Contact>();
	private static final HashMap<Integer, Contact> contactsByAddr = new HashMap<Integer, Contact>();
	private boolean active = false;
	private String trigger = null;
	private int addr = 0;
	
	public void trigger(int duration) throws IOException {
		activate(true);
		new Thread() {
			public void run() {
				try {
					sleep(duration);
					activate(false);
				} catch (Exception e) {}
			}
		}.start();
	}
	
	public void activate(boolean active) {		
		this.active = active;		
		if (active) {
			if (route == null) {
				plan.warn(this);
			} else {
				route.contact(this);
			}
		}
		try {
			stream();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Contact addr(int address) {
		addr = address;
		contactsByAddr.put(addr, this);
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
	
	public static Contact get(String contactId) {
		return contactsById.get(contactId);
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (addr > 0) json.put(ADDRESS, addr);
		return json;
	}
	
	public static Collection<Contact> list() {
		return contactsById.values();
	}

	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		if (json.has(ADDRESS)) addr(json.getInt(ADDRESS));
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
		String id = params.get(ID);
		if (action == null) return t("Missing ACTION on call to {}.process()",Contact.class.getSimpleName());
		Contact contact;
		switch (action) {
			case ACTION_ANALYZE:
				if (id == null) return t("Missing ID on call to {}.process()",Contact.class.getSimpleName());
				contact = contactsById.get(id);
				if (contact == null) return t("No contact with id {} found!",id);
				Tag propMenu = contact.propMenu();
				propMenu.children().insertElementAt(new Tag("div").content(t("Trigger a feedback sensor to assign it with this contact!")), 1);
				contact.plan.learn(contact);
				return propMenu;
		}
		return t("Unknown action: {}",action);
	}

	
	@Override
	public Form propForm(String formId) {
		Form form = super.propForm(formId);
		new Tag("h4").content(t("Hardware settings")).addTo(form);
		Tag label = new Input(ADDRESS, addr).addTo(new Label(t("Address:")+NBSP));
		Map<String, String> props = Map.of(REALM,REALM_CONTACT,ID,id(),ACTION,ACTION_ANALYZE);
		new Button(t("learn"), props).addTo(label).addTo(form);
		
		return form;
	}
	
	public void stream() throws IOException {
		Tag tag = super.tag(null);
		if (active) tag.clazz(tag.get("class")+" active");
		plan.stream("place "+tag);
	}


	public String trigger() {
		if (trigger == null) trigger = getClass().getSimpleName()+"-"+id();
		return trigger;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(ADDRESS)) addr(Integer.parseInt(params.get(ADDRESS)));
		return super.update(params);
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
}
