package de.srsoftware.web4rail;

import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public class LookupTable extends BaseClass{

	private static final String ADD_FORM = "add_lookup";
	private static final String COLUMNS = "columns";
	private static final String ROWS = "rows";

	public static Object action(Params params) {
		String action = params.getString(ACTION);
		if (isNull(action)) throw new NullPointerException(ACTION+" should not be null!");

		switch (action) {
			case ACTION_ADD:
				return addForm(params);
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

	private static Object create(String name, String cols, String rows2) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Select selector(String name) {
		Select selector = new Select(name);
		selector.addOption(REALM_LOCO, t("Locomotives"));
		selector.addOption(REALM_TRAIN, t("Trains"));
		selector.addOption(REALM_TRAIN+"_length", t("TrainLength"));
		return selector;
	}

}
