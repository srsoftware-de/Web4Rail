package de.srsoftware.web4rail;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
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
	private static final String NEW_ROW = "new_row";
	private static final String NEW_COL = "new_col";
	private static final String VALUES = "values";
	private static final String LENGTH = "length";
	private static final String CONFIRM = "confirm";
	private String colType;
	private String rowType;
	private String name;
	
	private Comparator<Object> comp = new Comparator<Object>() {
		
		@Override
		public int compare(Object a, Object b) {
			String sa = a.toString();
			String sb = b.toString();
			try {
				int ia = Integer.parseInt(sa);
				int ib = Integer.parseInt(sb);
				return ia-ib;
			} catch (NumberFormatException nfe) {}
			return sa.compareTo(sb);
		}
	};
	private TreeSet<Object> cols = new TreeSet<>(comp);
	private TreeSet<Object> rows = new TreeSet<>(comp);
	private TreeMap<Object,TreeMap<Object,Integer>> values = new TreeMap<>();
	

	private LookupTable() {}

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
			case ACTION_DROP:
				return isSet(table) ? table.drop(params) : plan.properties();
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
	

	private Window drop(Params params) {
		String confirm = params.getString(CONFIRM);
		if (CONFIRM.equals(confirm)) {
			this.remove();
			return plan.properties();
		}
		Tag div = new Tag("div").content(t("Are you sure you want to delete {}?",this));
		button(t("delete"), Map.of(ACTION,ACTION_DROP,CONFIRM,CONFIRM)).addTo(div);
		button(t("abort"), Map.of()).addTo(div);
		return properties(div.toString());
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(COLUMNS, colType);
		json.put(NAME, name);
		json.put(ROWS, rowType);
		json.put(VALUES, values);
		return json;
	}
	
	public static JSONArray jsonList() {
		JSONArray list = new JSONArray();
		listElements(LookupTable.class).forEach(table -> list.put(table.json()));
		return list;
	}
	
	@Override
	public LookupTable load(JSONObject json) {
		super.load(json);
		if (json.has(COLUMNS))  colType = json.getString(COLUMNS);
		if (json.has(ROWS))  rowType = json.getString(ROWS);
		if (json.has(NAME)) name = json.getString(NAME);
		if (json.has(VALUES)) {
			JSONObject vals = json.getJSONObject(VALUES);
			for (String rowKey : vals.keySet()) {
				if (LENGTH.equals(rowType)) {
					rows.add(rowKey);
				} else {
					BaseClass row = BaseClass.get(new Id(rowKey));
					if (isSet(row)) rows.add(row);
				}
				JSONObject columns = vals.getJSONObject(rowKey);
				for (String colKey : columns.keySet()) {
					TreeMap<Object, Integer> colVals = values.get(rowKey);
					if (isNull(colVals)) values.put(rowKey, colVals = new TreeMap<>());
					colVals.put(colKey, columns.getInt(colKey));
					
					if (LENGTH.equals(colType)) {
						cols.add(colKey);
					} else {
						BaseClass col = BaseClass.get(new Id(colKey));
						if (isSet(col)) cols.add(col);
					}
				}
			};
			LOG.debug("Values: {}",vals);
		}
		return this;
	}
	
	public static void loadAll(JSONArray list) {
		for (int i=0; i<list.length(); i++) {
			new LookupTable().load(list.getJSONObject(i));
		}
	}


	
	public String name() {
		return name;
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm, String... errorMessages) {
		formInputs.add(t(NAME),new Input(NAME,name));
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
		selector.addOption(LENGTH, t("TrainLength"));
		return selector;
	}
	
	private void setValue(String key, Object value) {
		try {
			Integer intVal = Integer.parseInt(value.toString());
			key = key.substring(1,key.length()-1);
			String[] parts = key.split("\\]\\[",2);
			String rowKey = parts[0];
			String colKey = parts[1];
			LOG.debug("Setting value of {}/{} to {}",rowKey,colKey,intVal);
			TreeMap<Object, Integer> entries = values.get(rowKey);
			if (isNull(entries)) values.put(rowKey, entries = new TreeMap<>());
			entries.put(colKey, intVal);
		} catch (NumberFormatException nfe) {
			LOG.debug("invalid value: {}",value,nfe);
		}
		
	}
	
	private Fieldset table() {		
		Fieldset fieldset = new Fieldset(t("Values"));		
		
		Table table = new Table();
		Vector<Object> head = new Vector<Object>();
		head.add("");
		if (LENGTH.equals(colType)) {
			for (Object col : cols) head.add("&lt; "+col);
		} else head.addAll(cols);
		
		table.addHead(head.toArray());
		boolean prefix = LENGTH.equals(rowType);
		for (Object row : rows) { // Zeilen
			if ("".equals(row)) continue;
			Vector<Object> entries = new Vector<>();
			entries.add((prefix ? "&lt; ":"")+row);
			String rowId = (row instanceof BaseClass ? ((BaseClass)row).id() : row).toString();
			
			TreeMap<Object, Integer> items = values.get(rowId);		
			
			for (Object col : cols) { // Spalten
				if ("".equals(col)) continue;
				String colId = (col instanceof BaseClass ? ((BaseClass)col).id() : col).toString();
				Object value = isSet(items) ? items.get(colId) : null;
				
				Input input = isSet(value) ? new Input("value["+rowId+"]["+colId+"]", value) : new Input("value["+rowId+"]["+colId+"]");
				
				entries.add(input.numeric());
			}
			
			table.addRow(entries.toArray());
		}
		
		
		Form form = table.addTo(rowAdder(new Form(id()+"_values")));
		new Input(REALM,REALM_LOOKUP).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);

		new Button(t("Apply"), form).addTo(form);
		button(t("Delete"),Map.of(ACTION,ACTION_DROP)).addTo(form);
		
		return form.addTo(fieldset);
	}

	private Form rowAdder(Form form) {
		Tag select = null;
		switch (colType) {
			case REALM_CAR:
				select = Car.selector(null, null).attr(NAME, NEW_COL);
				break;
			case REALM_TRAIN:
				select = Train.selector(null, null).attr(NAME, NEW_COL);
				break;
			case LENGTH:
				select = new Input(NEW_COL);
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
			case LENGTH:
				select = new Input(NEW_ROW);
				break;
		}
		if (isSet(select)) select.addTo(new Label(t("add row ({})",t(rowType))+':'+NBSP)).addTo(form);
		return form;
	}

	@Override
	public String toString() {
		return t("{} \"{}\"",getClass().getSimpleName(),name);
	}

	@Override
	protected Object update(Params params) {
		if (params.containsKey(NAME)) name = params.getString(NAME);
		if (params.containsKey(NEW_COL)) {
			if (LENGTH.equals(colType)) {
				cols.add(params.getString(NEW_COL));
			} else {
				Object o = BaseClass.get(Id.from(params, NEW_COL));
				if (isSet(o) && !cols.contains(o)) cols.add(o);
			}
		}
		if (params.containsKey(NEW_ROW)) { 
			if (LENGTH.equals(rowType)){
				rows.add(params.getString(NEW_ROW));
			} else {			
				Object o = BaseClass.get(Id.from(params, NEW_ROW));
				if (isSet(o) && !rows.contains(o)) rows.add(o);
			}
		}
		for (Entry<String, Object> entry : params.entrySet()) {
			String key = entry.getKey();			
			if (key.startsWith("value[")) setValue(key.substring(5),entry.getValue());
		}

		super.update(params);
		return properties();
	}
}
