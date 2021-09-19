package de.srsoftware.web4rail.actions;

import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.LookupTable;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Store;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Window;

public class LookupValue extends Action {
	
	private static final String TABLE = "table";
	private static final String STORE = "store";
	private LookupTable lookupTable = null;
	private Store store = null;

	public LookupValue(BaseClass parent) {
		super(parent);
	}
	
	@Override
	public boolean fire(Context context) {
		try {
			if (!isSet(store,lookupTable)) return false;
			int value = lookupTable.getValue(context);
			store.setValue(value);			
		} catch (NullPointerException npe) {
			return false;
		}
		return true;
	}

	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(lookupTable)) json.put(TABLE, lookupTable.id());
		if (isSet(store)) json.put(STORE, store.name());
		return json;
	}
	

	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(TABLE)) new LoadCallback() {			
			@Override
			public void afterLoad() {
				lookupTable = LookupTable.get(new Id(json.getString(TABLE)));		
			}
		};		
		
		if (json.has(STORE)) store = Store.get(json.getString(STORE));
		return this;		
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Lookup table"),LookupTable.selector(lookupTable, null));
		formInputs.add(t("Store"), new Input(STORE,isSet(store)?store.name():""));
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		if (isSet(store,lookupTable)) return t("Set {} to value from {}",store.name(),lookupTable);
		return t("[Click here to setup look-up]");
	}
		
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(LookupTable.class.getSimpleName())) {
			Id tableId = Id.from(params, LookupTable.class.getSimpleName());
			if (tableId.equals(0)) {
				lookupTable = null;
			} else {
				LookupTable newTable = LookupTable.get(tableId);
				if (isSet(newTable)) lookupTable = newTable;
			}
			
		}
		
		String storeName = params.getString(STORE);
		if (isSet(storeName) && !storeName.isEmpty()) store = Store.get(storeName);
		return super.update(params);
	}
}
