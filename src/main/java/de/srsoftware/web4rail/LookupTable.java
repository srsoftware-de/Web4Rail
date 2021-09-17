package de.srsoftware.web4rail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.Window;

public class LookupTable extends BaseClass{

	private static final String ADD_FORM = "add_lookup";
	private static final String COLUMNS = "columns";
	private static final String ROWS = "rows";
	private static final String DEFAULT_VALUE = "default";
	private static final String NEW_ROW = "new_row";
	private static final String NEW_COL = "new_col";
	private String colType;
	private String rowType;
	private String name;
	private int defaultValue = 0;
	
	private HashSet<Object> cols = new HashSet<>();
	private HashSet<Object> rows = new HashSet<>();
	private HashMap<Object,HashMap<Object,Integer>> values = new HashMap<>();

	public LookupTable(String name, String cols, String rows) {
		this.name = name;
		this.colType = cols;
		this.rowType = rows;
	}

	public static Object action(Params params) {
		String action = params.getString(ACTION);
		if (isNull(action)) throw new NullPointerException(ACTION+" should not be null!");
		LookupTable table = get(Id.from(params));
		switch (action) {
			case ACTION_ADD:
				return addForm(params);
			case ACTION_PROPS:
				return isSet(table) ? table.properties() : plan.properties();
			case ACTION_UPDATE:
				return isSet(table) ? table.update(params) : plan.properties();
		}
		return plan.properties(t("Unknown action: {}",action));
	}

	private static Object addForm(Params params) {
		String name = params.getString(NAME);
		String cols = params.getString(COLUMNS);
		String rows = params.getString(ROWS);
		if (isSet(name,cols,rows)) return create(name,cols,rows);
		Window win = new Window(ADD_FORM, t("add lookup table"));
		FormInput formInputs = new FormInput();
		
		formInputs.add(t(NAME), new Input(NAME, t("lookup")+'-'+timestamp()));
		
		formInputs.add(t(ROWS), selector(ROWS));
		formInputs.add(t(COLUMNS), selector(COLUMNS));
		Form form = form(ADD_FORM+"_form", formInputs);
		new Input(REALM, REALM_LOOKUP).hideIn(form);
		new Input(ACTION, ACTION_ADD).hideIn(form);
		return form.addTo(win);
	}
	
	private static Object create(String name, String cols, String rows) {
		return new LookupTable(name, cols, rows).register().properties();
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(COLUMNS, colType);
		json.put(DEFAULT_VALUE, defaultValue);
		json.put(NAME, name);
		json.put(ROWS, rowType);
		return json;
	}
	
	public static JSONArray jsonList() {
		JSONArray list = new JSONArray();
		listElements(LookupTable.class).forEach(table -> list.put(table.json()));
		return list;
	}

	
	public String name() {
		return name;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm, String... errorMessages) {
		formInputs.add(t(NAME),new Input(NAME,name));
		formInputs.add(t("default value"),new Input(DEFAULT_VALUE, defaultValue).numeric().title(t("This value is used, when no entry is set for a specific input pair.")));
		
		postForm.add(table());
		
		return super.properties(preForm, formInputs, postForm, errorMessages);
	}
	
	@Override
	public String realm() {
		return REALM_LOOKUP;
	}

	private static Select selector(String name) {
		Select selector = new Select(name);
		selector.addOption(REALM_CAR, t("Cars"));
		selector.addOption(REALM_TRAIN, t("Trains"));
		selector.addOption(REALM_TRAIN+"_length", t("TrainLength"));
		return selector;
	}
	
	private Fieldset table() {
		
		Fieldset fieldset = new Fieldset(t("Values"));
		
		rowAdder().addTo(fieldset);
		
		Table table = new Table();
		Vector<Object> head = new Vector<Object>();
		head.add("");
		head.addAll(cols);
		table.addHead(head.toArray());
		for (Object row : rows) {
			Vector<Object> entries = new Vector<>();
			entries.add(row);
			
			HashMap<Object, Integer> items = values.get(row);
			for (Object col : cols) {
				Integer item = isSet(items) ? items.get(col) : defaultValue;
				entries.add(item);
			}
			table.addRow(entries.toArray());
		}
		
		return table.addTo(fieldset);
	}	

	private Tag rowAdder() {
		Fieldset fieldset = new Fieldset("Add column/row");
		Form form = new Form("add_row_form");
		new Input(REALM,REALM_LOOKUP).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		Tag select = null;
		switch (colType) {
			case REALM_CAR:
				select = Car.selector(null, null).attr(NAME, NEW_COL);
				break;
			case REALM_TRAIN:
				select = Train.selector(null, null).attr(NAME, NEW_COL);
				break;
		}		
		if (isSet(select)) select.addTo(new Label(t("add column ({})",t(colType))+':'+NBSP)).addTo(form);

		select = null;
		switch (rowType) {
			case REALM_CAR:
				select = Car.selector(null, null).attr(NAME, NEW_ROW);
				break;
			case REALM_TRAIN:
				select = Train.selector(null, null).attr(NAME, NEW_ROW);
				break;
		}
		if (isSet(select)) select.addTo(new Label(t("add row ({})",t(rowType))+':'+NBSP)).addTo(form);
		return new Button(t("add"), form).addTo(form).addTo(fieldset);
	}

	@Override
	public String toString() {
		return t("{} \"{}\"",getClass().getSimpleName(),name);
	}

	@Override
	protected Object update(Params params) {
		if (params.containsKey(NAME)) name = params.getString(NAME);
		if (params.containsKey(DEFAULT_VALUE)) defaultValue = params.getInt(DEFAULT_VALUE);
		if (params.containsKey(NEW_COL)) {
			Object o = BaseClass.get(Id.from(params, NEW_COL));
			if (isSet(o) && !cols.contains(o)) cols.add(o);
		}
		if (params.containsKey(NEW_ROW)) {
			Object o = BaseClass.get(Id.from(params, NEW_ROW));
			if (isSet(o) && !rows.contains(o)) rows.add(o);
		}

		super.update(params);
		return properties();
	}
}
