package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Store;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class SetValue extends Action {
	
	private static final String VALUE = "value";
	private static final String STORE = "store";
	private String value = null;
	private Store store = null;

	public SetValue(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		try {
			if (!isSet(store,value)) return false;
			store.setValue(value);			
		} catch (NullPointerException npe) {
			return false;
		}
		return true;
	}

	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(value)) json.put(VALUE, value);
		if (isSet(store)) json.put(STORE, store.name());
		return json;
	}
	

	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(VALUE)) value = json.getString(VALUE);		
		if (json.has(STORE)) store = Store.get(json.getString(STORE));
		return this;		
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Store"), new Input(STORE,isSet(store)?store.name():""));
		formInputs.add(t("Value"), new Input(VALUE,isSet(value)?value:0));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isSet(store,value)) return t("Set \"{}\" to \"{}\"",store.name(),value);
		return "["+t("Click here to setup assignment")+"]";
	}
		
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		String newValue = params.getString(VALUE);		
		if (isSet(newValue)) value = newValue;
		
		String storeName = params.getString(STORE);
		if (isSet(storeName) && !storeName.isEmpty()) store = Store.get(storeName);
		return super.update(params);
	}
}
