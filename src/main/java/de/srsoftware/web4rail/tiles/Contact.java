package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.EventListener;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.threads.DelayedExecution;

public class Contact extends Tile{
	private static final String ADDRESS = "address";
	private static final HashMap<Integer, Contact> contactsByAddr = new HashMap<Integer, Contact>();
	private boolean state = false;
	private String trigger = null;
	protected int addr = 0;
	private ActionList actions;
	private OffTimer timer = null;
	private HashSet<EventListener> listeners = new HashSet<EventListener>();

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
			super(Application.threadName("OffTimer("+Contact.this+")"));
			start();
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
			state = true;
			stream();
			if (isSet(timer)) timer.abort();
			Train train = lockingTrain();
			Context context = isSet(train) ? train.contact(this) : new Context(this);
			actions.fire(context);
			
			for (EventListener listener : listeners) listener.fire();
			
			plan.alter();
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

	public void addListener(EventListener l) {
		listeners.add(l);
	}

	@Override
	public Object click(boolean shift) throws IOException {
		if (!(isDisabled() || shift)) trigger(10);
		return super.click(shift);
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
	
	public static Object process(Params params) {
		String action = params.getString(ACTION);
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
			case ACTION_PROPS:
				return contact.properties();
			case ACTION_UPDATE:
				return plan.update(params);
		}
		return t("Unknown action: {}",action);
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Tag span = new Tag("span");
		new Input(ADDRESS, addr).numeric().addTo(span).content(NBSP);
		button(t("learn"),Map.of(ACTION,ACTION_ANALYZE)).addTo(span);
		formInputs.add(t("Address"),span);
		
		Fieldset fieldset = new Fieldset(t("Actions")).id("props-actions");
		actions.listAt(fieldset);
		postForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public void removeChild(BaseClass child) {
		if (child == actions) actions = null;
		super.removeChild(child);
	}
	
	public void removeListener(EventListener listener) {
		listeners.remove(listener);
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
	
	@Override
	public String toString() {
		return t("Contact")+"("+x+","+y+")";
	}

	public String trigger() {
		if (trigger == null) trigger = getClass().getSimpleName()+"-"+id();
		return trigger;
	}
	
	public boolean trigger(int duration) {
		new DelayedExecution(duration,"Contact("+Contact.this.addr+")") {
			
			@Override
			public void execute() {
				activate(false);
			}
		};
		activate(true);
		return true;
	}
	@Override
	public Tile update(Params params) {
		if (params.containsKey(ADDRESS)) addr(params.getInt(ADDRESS));
		return super.update(params);
	}
}
