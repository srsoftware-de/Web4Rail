package de.srsoftware.web4rail.conditions;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.tools.translations.Translation;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public abstract class Condition extends BaseClass {
	public static final Logger LOG = LoggerFactory.getLogger(Condition.class);
	protected static final String INVERTED = "inverted";
	private static final String PREFIX = Condition.class.getPackageName();
	public boolean inverted = false;

	public Condition() {
		this(new Id());
	}
	
	public Condition(Id id) {
		this.id = id;
		register();
	}	
	
	public static Object action(Params params,Plan plan) {
		String action = params.getString(ACTION);
		if (action == null) return t("No action passed to Condition.action!");
		
		Id id = Id.from(params);
		Condition condition = BaseClass.get(id);
		
		switch (action) {
			case ACTION_ADD:
				return addCondition(params);
			case ACTION_DROP:
				BaseClass context = condition.context();
				condition.remove();
				return context.properties();
			case ACTION_PROPS:
				return condition.properties();
			case ACTION_UPDATE:
				Object res = condition.update(params);
				if (res instanceof Window) {
					BaseClass parent = condition.parent();
					while (isSet(parent)) {
						if (parent instanceof ActionList) {
							((Window) res).highlight(parent);
							break;
						}
						parent = parent.parent();						
					}
				}
				return res;				
		}
		return t("Unknown action: {}",action);
	}
	
	private static Object addCondition(Params params) {
		String type = params.getString(REALM_CONDITION);
		if (isNull(type)) return t("No type supplied to addCondition!");
		
		Id parentId = Id.from(params);
		if (isNull(parentId)) return t("No parent id supplied to addCondition");
		
		ConditionList conditionList = BaseClass.get(parentId);
		if (isNull(conditionList)) return t("No condition list with id {} found!",parentId);
		
		Condition condition = Condition.create(type);
		conditionList.add(condition);
		return condition.properties();
	}

	public static Condition create(String type) {
		if (isNull(type)) return null;
		try {
			return (Condition) Class.forName(PREFIX+"."+type).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public BaseClass context() {
		BaseClass context = this;
		while (isSet(context.parent()) && (context instanceof Condition || context instanceof Action)) {
			context = context.parent();
		}
		return context;
	}
	
	public abstract boolean fulfilledBy(Context context);
	
	public void inversionOption(FormInput formInputs) {
		formInputs.add(t("inverted"),new Checkbox(INVERTED, t("inverted"), inverted));
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject().put(TYPE, getClass().getSimpleName());
		if (inverted) json.put(INVERTED, true);
		return json;
	}
	
	/**
	 * If arguments are given, the first is taken as content, the second as tag type.
	 * If no content is supplied, name is set as content.
	 * If no type is supplied, "span" is preset.
	 * @param args
	 * @return
	 */
	public Tag link(String...args) {
		String tx = args.length<1 ? toString()+NBSP : args[0];
		String type = args.length<2 ? "span" : args[1];
		String context = args.length<3 ? null : args[2];
		return link(type, tx,Map.of(CONTEXT,context));
	}
		
	private static List<Class<? extends Condition>> list() {
		return List.of(
				AndCondition.class,
				AutopilotActive.class,
				BlockFree.class,
				CarInTrain.class,
				CarOrientation.class,
				OrCondition.class,
				PushPullTrain.class,
				RouteEndBlock.class,
				StoreHasValue.class,
				SwitchIsOn.class,
				TrainHasTag.class,
				TrainIsShunting.class,
				TrainLength.class,
				TrainSelect.class,
				TrainSpeed.class,
				TrainWasInBlock.class);
	}
	
	public Condition load(JSONObject json) {
		inverted = json.has(INVERTED) && json.getBoolean(INVERTED);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		inversionOption(formInputs);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	public static Select selector() {
		Select select = new Select(REALM_CONDITION);
		TreeMap<String, String> names = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		
		for (Class<? extends Condition> clazz : list()) {
			String s = t(clazz.getSimpleName());
			names.put(s, clazz.getSimpleName());
		}
		
		for (Entry<String, String> entry : names.entrySet()) select.addOption(entry.getValue(), entry.getKey());
		return select;
	}
	
	public static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
		
	@Override
	public String toString() {
		return t("invalid condition");
	}

	protected Object update(Params params) {
		inverted = "on".equals(params.get(INVERTED));
		super.update(params);
		return context().properties();
	}
}
