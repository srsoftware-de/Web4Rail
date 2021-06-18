package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Contact;

public class TriggerContact extends Action {
		
	public TriggerContact(BaseClass parent) {
		super(parent);
	}

	private Contact contact = null;
	
	@Override
	public boolean fire(Context context) {
		if (isSet(contact)) return contact.trigger(200);
		return false;
	}
	
	@Override
	protected String highlightId() {
		return isSet(contact) ? contact.id().toString() : null;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select contact")+": "+(isNull(contact) ? t("unset") : contact),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,CONTACT)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == contact) contact = null;
		super.removeChild(child);
	}
	
	public String toString() {
		return isSet(contact) ? t("Trigger {}",contact) : "["+t("Click here to setup contact")+"]";
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		Id contactId = Id.from(params,CONTACT);
		if (isSet(contactId)) contact = Contact.get(contactId);
		return properties();
	}

}
