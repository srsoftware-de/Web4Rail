package de.srsoftware.web4rail.actions;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Switch;
import de.srsoftware.web4rail.tiles.Tile;

public class TriggerContact extends Action {
		
	public TriggerContact(BaseClass parent) {
		super(parent);
	}

	private Tile contactOrSwitch = null;
	
	@Override
	public boolean fire(Context context) {
		if (contactOrSwitch instanceof Contact) return ((Contact)contactOrSwitch).trigger(200);
		if (contactOrSwitch instanceof Switch) return ((Switch)contactOrSwitch).trigger(context);
		return false;
	}
	
	@Override
	protected String highlightId() {
		return isSet(contactOrSwitch) ? contactOrSwitch.id().toString() : null;
	}
	
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (contactOrSwitch instanceof Contact) json.put(CONTACT, contactOrSwitch.id());
		if (contactOrSwitch instanceof Switch) json.put(SWITCH, contactOrSwitch.id());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
			
		if (json.has(CONTACT)) new LoadCallback() {
			@Override
			public void afterLoad() {
				contactOrSwitch = Contact.get(Id.from(json,CONTACT));
			}
		};
 
		if (json.has(SWITCH)) new LoadCallback() {
			@Override
			public void afterLoad() {
				contactOrSwitch = Switch.get(Id.from(json,SWITCH));
			}
		};

		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select contact or switch")+": "+(isNull(contactOrSwitch) ? t("unset") : contactOrSwitch),button(t("Select from plan"),Map.of(ACTION,ACTION_UPDATE,ASSIGN,CONTACT)));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		if (child == contactOrSwitch) contactOrSwitch = null;
		super.removeChild(child);
	}
	
	public String toString() {
		return isSet(contactOrSwitch) ? t("Trigger {}",contactOrSwitch) : "["+t("Click here to setup contact/switch")+"]";
	};
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		Id contactId = Id.from(params,CONTACT);
		String error = null;
		if (isSet(contactId)) {
			Tile tile = BaseClass.get(contactId);
			if (tile instanceof Contact || tile instanceof Switch) {
				contactOrSwitch = tile;
			} else error = t("{} is neither a contact nor a switch!",tile);
		}
		return context().properties(error);
	}

}
