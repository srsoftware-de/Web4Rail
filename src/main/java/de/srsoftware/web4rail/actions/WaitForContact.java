package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.DelayedExecution;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Contact.Listener;
import de.srsoftware.web4rail.tiles.Tile;

public class WaitForContact extends ActionList {

	private static final String TIMEOUT = "timeout";
	private static final String TIMEOUT_ACTIONS = "timeout_actions";
	ActionList timeoutActions = new ActionList(this);
	boolean fired = false;
	public WaitForContact(BaseClass parent) {
		super(parent);
	}

	private Contact contact = null;
	private int timeout = 10000;
	
	@Override
	public boolean fire(Context context,Object cause) {
		LOG.debug("{}.fire(...) called, waiting for {}.",this,contact);
		if (isNull(contact)) return false;
		fired = false;
		Listener listener = new Listener() {
			@Override
			public void fired(Object cause) {
				LOG.debug("{} triggered, firing {}",contact,actions);
				fired = true;
				contact.removeListener(this);
				WaitForContact.super.fire(context,cause);
			}
		};		
		contact.addListener(listener);
		new DelayedExecution(timeout,cause) {
			
			@Override
			public void execute() {
				contact.removeListener(listener);
				LOG.debug("{} timed out, firing {}",this,timeoutActions);
				if (!fired) timeoutActions.fire(context,cause);
			}
		};
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(contact)) json.put(CONTACT, contact.id());
		json.put(TIMEOUT, timeout);
		if (!timeoutActions.isEmpty()) {
			json.put(TIMEOUT_ACTIONS, timeoutActions.json());
		}
		return json;
	}
	
	@Override
	public Tag list() {
		Tag list = super.list();
		for (Tag child : list.children()) {
			if (child.is("ol")) {
				break;
			}
		}
		timeoutActions.list().addTo(new Tag("span").content(t("On timeout (after {} ms)",timeout)+":")).addTo(list);
		
		return list;
	}
	
	@Override
	public Action load(JSONObject json) {
		if (json.has(CONTACT)) {
			String cid = json.getString(CONTACT);
			contact = BaseClass.get(new Id(cid));
			if (isNull(contact)) new DelayedExecution(this) {
				
				@Override
				public void execute() {
					contact = BaseClass.get(new Id(cid));
				}
			};
		}
		if (json.has(TIMEOUT)) timeout = json.getInt(TIMEOUT);
		if (json.has(TIMEOUT_ACTIONS)) timeoutActions.load(json.getJSONObject(TIMEOUT_ACTIONS));
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Contact")+": "+(isNull(contact) ? t("unset") : contact),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,CONTACT)));
		formInputs.add(t("Timeout"),new Input(TIMEOUT,timeout).numeric().addTo(new Tag("span")).content(NBSP+"ms"));
		
		Fieldset fieldset = new Fieldset(t("Actions on timeout"));
		fieldset.id("actions");
		timeoutActions.list().addTo(fieldset);
		postForm.add(fieldset);

		return super.properties(preForm, formInputs, postForm);
	}
	
	@Override
	public String toString() {
		return isSet(contact) ? t("Wait for {}, then",contact) : "["+t("Click here to setup contact")+"]";
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		if (params.containsKey(CONTACT)) {
			Tile tile = BaseClass.get(new Id(params.get(CONTACT)));
			if (tile instanceof Contact) {
				contact = (Contact) tile;
			} else return t("Clicked tile is not a {}!",t("contact"));
		}
		if (params.containsKey(TIMEOUT)) timeout = Integer.parseInt(params.get(TIMEOUT));
		return super.update(params);
	}
}
