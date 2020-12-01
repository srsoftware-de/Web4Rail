package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Contact;

public class TriggerContact extends Action {
		
	private Contact contact = null;
	
	@Override
	public boolean fire(Context context) {
		if (isSet(contact)) return contact.trigger(200);
		return false;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(contact)) json.put(CONTACT, contact.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		Id contactId = Id.from(json,CONTACT);
		if (isSet(contactId)) contact = Contact.get(contactId);
		return this;
	}
	
	@Override
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		
		Select select = Contact.selector(contact);
		select.addTo(new Label(t("Select contact:")+NBSP)).addTo(form);
		
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	public String toString() {
		return isSet(contact) ? t("Trigger {}",contact) : "["+t("click here to setup contact")+"]";
	};
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		Id contactId = Id.from(params,CONTACT);
		if (isSet(contactId)) contact = Contact.get(contactId);
		return properties(params);
	}

}
