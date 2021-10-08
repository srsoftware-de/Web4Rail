package de.srsoftware.web4rail.conditions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Store;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class StoreHasValue extends Condition {
	
	private static final String VALUE = "value";
	private static final String STORE = "store";
	private String value = null;
	private Integer intValue = null;
	private Store store = null;
	
	private boolean evaluate() {
		if (!isSet(store,value)) return false;
		if (isSet(intValue)) return store.intValue() == intValue;
		return value.equals(store.value());
	}
	
	@Override
	public boolean fulfilledBy(Context context) {
		return evaluate() != inverted;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(value)) json.put(VALUE, value);
		if (isSet(store)) json.put(STORE, store.name());
		return json;
	}
	
	public Condition load(JSONObject json) {
		super.load(json);
		if (json.has(STORE)) store = Store.get(json.getString(STORE));
		if (json.has(VALUE)) {
			value = json.getString(VALUE);
			try {
				intValue = Integer.parseInt(value);
			} catch (NumberFormatException nfe) {}
		}
		return this;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Select store to read from:"),Store.selector(store));
		formInputs.add(t("Value"),new Input(VALUE, isSet(value) ? value : ""));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isNull(store)) return "["+t("Click setup store lookup")+"]";
		return t(inverted ? "Store \"{}\" does not have value \"{}\"" : "Store \"{}\" has value \"{}\"",store.name(),value) ;
	}

	@Override
	protected Object update(Params params) {
		String storeName = params.getString(Store.class.getSimpleName());
		if (isSet(storeName)) store = Store.get(storeName);
		
		String newVal = params.getString(VALUE);
		if (isSet(newVal)) value = newVal;
		return super.update(params);
	}
}
