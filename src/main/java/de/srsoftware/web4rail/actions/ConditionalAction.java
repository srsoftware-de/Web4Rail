package de.srsoftware.web4rail.actions;

import static de.srsoftware.web4rail.Constants.TYPE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.conditions.TrainSelect;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tiles.Contact;

public class ConditionalAction extends Action {
	
	private Vector<Condition> conditions = new Vector<Condition>();
	private Vector<Action> actions = new Vector<Action>();

	private ConditionalAction addCondition(Condition condition) {
		conditions.add(new TrainSelect());
		return this;
	}
	
	private static void addToContact(Route route, Contact contact, String conditionType) {
		Condition condition = null;
		switch (conditionType) {
			case "TrainSelect":
				condition = new TrainSelect();
				break;
			default: return;
		}
		route.addAction(contact.trigger(), new ConditionalAction().addCondition(condition));
	}
	
	@Override
	public boolean fire(Context context) throws IOException {
		for (Condition condition : conditions) {
			if (condition.fulfilledBy(context)) return fireActions(context);
		}
		return false;		
	}

	private boolean fireActions(Context context) {
		for (Action action : actions) try {
			action.fire(context);
		} catch (IOException e) {
			LOG.warn("Was not able to fire {}",action);
		}
		return true;
	}
	
	public static Window propForm(HashMap<String, String> params, Route route, Contact contact) {
		String condition = params.get(REALM_CONDITION);
		if (condition != null) {
			addToContact(route,contact,condition);
			return route.properties();		
		}
		Window win = Action.propForm(params);
		String formId = "add-action-to-contact-"+contact.id();
		Tag form = new Form(formId);
		new Tag("div").content(t("Add Action {} to contact {} on route {}:",ConditionalAction.class.getSimpleName(),contact,route)).addTo(win);
		new Input(REALM, REALM_ROUTE).hideIn(form);
		new Input(ID,route.id()).hideIn(form);
		new Input(ACTION,ACTION_ADD_ACTION).hideIn(form);
		new Input(CONTACT,contact.id()).hideIn(form);
		new Input(TYPE,ConditionalAction.class.getSimpleName()).hideIn(form);
		Select select = new Select(REALM_CONDITION);
		List<Class<? extends Condition>> conditionTypes = List.of(TrainSelect.class);
		for (Class<? extends Condition> clazz : conditionTypes) select.addOption(clazz.getSimpleName());
		select.addTo(form);
		new Button(t("Create action"),"return submitForm('"+formId+"');").addTo(form).addTo(win);
		return win;
	}
	
	@Override
	public String toString() {
		if (conditions.isEmpty()) return t("Invalid condition");
		StringBuffer sb = new StringBuffer();		
		for (int i = 0; i<conditions.size(); i++) {
			Condition condition = conditions.get(i);
			Tag link = condition.link("span");
			sb.append(link);
		}
		return t("if ({}):",sb);
	}
}
