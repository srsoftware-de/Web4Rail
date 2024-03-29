package de.srsoftware.web4rail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.tools.translations.Translation;
import de.srsoftware.web4rail.History.LogEntry;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.functions.Function;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.TextArea;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

/**
 * @author Stephan Richter, SRSoftware 2020…2021 
 */
public abstract class BaseClass implements Constants{
	public static Plan plan; // the track layout in use
	public static final Random random = new Random();
	public static String speedUnit = DEFAULT_SPEED_UNIT;
	public static String lengthUnit = DEFAULT_LENGTH_UNIT;
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
	protected Id id = null;
	protected String notes;
	private static HashMap<Id,BaseClass> registry = new HashMap<BaseClass.Id, BaseClass>();
	private static HashMap<Class<? extends BaseClass>,Set<String>> customFieldNames = new HashMap<Class<? extends BaseClass>, Set<String>>();
	
	public static final Logger LOG = LoggerFactory.getLogger(BaseClass.class);
	private static final String CUSTOM_FIELDS = "custom_Fields";
	private static final String NEW_CUSTOM_FIELD_NAME = "new_custom_field_name";
	protected static final String PROPS_BASIC = "props-basic";
	protected HashMap<String,String> customFieldValues = new HashMap<String, String>();
	private BaseClass parent;
	
	public static class Context {

		private BaseClass main = null;
		private Tile tile;
		private Block block;
		private Train train;
		private Route route;
		private Action action;
		private Condition condition;
		private Car car;
		private Contact contact;
		private Direction direction;
		private Integer waitTime;
		EventListener invalidationListener = null;
		
		public Context(BaseClass object) {
			setMain(object);
		}
		
		public Action action() {
			return action;
		}
		
		public Block block() {
			return block;
		}
		
		public Context block(Block newBlock) {
			//LOG.debug("{}.block({})",this,newBlock);
			block = newBlock;
			return this;
		}

		public Car car() {
			return car;
		}
		
		public Context car(Car newCar) {
			car = newCar;
			return this;
		}
		
		public void clear() {
			action = null;
			block = null;
			car = null;
			condition = null;
			contact = null;
			direction = null;
			main = null;
			route = null;
			tile = null;
			train = null;
			waitTime = null;
		}
		
		public Context clone() {
			Context clone = new Context(main);
			clone.action = action;
			clone.block = block;
			clone.car = car;
			clone.condition = condition;
			clone.contact = contact;
			clone.direction = direction;
			clone.route = route;
			clone.tile = tile;
			clone.train = train;
			clone.waitTime = waitTime;
			return clone;
		}
		
		public Condition condition() {
			return condition;
		}
		
		public Contact contact() {
			return contact;
		}
		
		public Context contact(Contact newContact) {
			contact = newContact;
			return this;
		}

		
		public Direction direction() {
			return direction;
		}

		public Context direction(Direction newDirection) {
			//LOG.debug("{}.direction({})",this,newDirection);
			direction = newDirection;
			return this;
		}
		
		public void invalidate() {
			setMain(null);
			if (isSet(invalidationListener)) invalidationListener.fire();
		}
		
		public boolean invalidated() {
			return isNull(main);
		}
		
		public void onInvalidate(EventListener listener) {
			invalidationListener = listener;
		}

		
		public Context setMain(BaseClass object) {
			main = object;
			//LOG.debug("{}.setMain({})",this,object);
			if (main instanceof Tile) this.tile = (Tile) main;
			if (main instanceof Contact) this.contact = (Contact) main;
			if (main instanceof Block) this.block = (Block) main;
			if (main instanceof Train) this.train = (Train) main;
			if (main instanceof Route) this.route = (Route) main;
			if (main instanceof Action) this.action = (Action) main;
			if (main instanceof Condition) this.condition = (Condition) main;
			if (main instanceof Car) this.car = (Car) main;
			return this;		
		}
				
		public Route route() {
			return route;
		}
		
		public Context route(Route newRoute) {
			route = newRoute;
			return this;
		}
		
		public Tile tile() {
			return tile;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer(getClass().getSimpleName());
			sb.append("(");
			sb.append("main: "+main);
			if (isSet(train)) sb.append(", "+t("Train: {}",train));
			if (isSet(direction)) sb.append(", "+t("Direction: {}",direction));
			if (isSet(block)) sb.append(", "+t("Block: {}",block));
			if (isSet(car))   sb.append(", "+t("Fahrzeug: {}",car));
			if (isSet(route))   sb.append(", "+t("Route: {}",route));
			if (isSet(contact)) sb.append(", "+t("Contact: {}",contact));
			if (isSet(waitTime)) sb.append(", "+t("Wait time: {} ms",waitTime));
			sb.append(")");
			return sb.toString();
		}
		
		public Train train() {
			return train;
		}

		public Context train(Train newTrain) {
			//LOG.debug("{}.train({})",this,newTrain);
			train = newTrain;
			return this;
		}
		
		public Integer waitTime() {
			return waitTime;
		}

		public void waitTime(int ms) {
			waitTime = ms;
		}
	}
	
	public static class FormInput extends ArrayList<Map.Entry<String, Tag>>{
		
		private static final long serialVersionUID = -2371203388908395216L;

		public FormInput add(String caption,Tag tag) {
			add(new AbstractMap.SimpleEntry<String,Tag>(caption,tag));
			return this;
		}
	}
	
	public static class Id implements CharSequence, Comparable<Id>{
		private String internalId;

		public Id() {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			internalId = ""+new Date().getTime();
		}
		
		
		public Id(String id) {
			internalId = id;
		}
		
		@Override
		public int hashCode() {
			return internalId.hashCode();
		}
		
		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (this == other) return true;
			return internalId.equals(other.toString());
		}

		public static Id from(JSONObject json) {
			return Id.from(json,ID);
		}
		
		public static Id from(JSONObject json,String key) {
			return json.has(key) ? new Id(""+json.get(key)) : null;
		}
		
		public static Id from(Params params) {
			return Id.from(params,ID);
		}


		public static Id from(Params params, String key) {
			String sid = params.getString(key);
			return sid == null ? null : new Id(sid);
		}

		@Override
		public char charAt(int index) {
			return internalId.charAt(index);
		}


		@Override
		public int length() {
			return internalId.length();
		}


		@Override
		public CharSequence subSequence(int begin, int end) {
			return internalId.subSequence(begin, end);
		}
		
		@Override
		public String toString() {
			return internalId;
		}


		@Override
		public int compareTo(Id other) {
			return internalId.compareTo(other.internalId);
		}
	}
	

	public Window addLogEntry(String text) {
		History.assign(new History.LogEntry(text),this);
		return properties();
	}
	
	public Button button(String text,Map<String,String> additionalProps) {
		return new Button(text,props(additionalProps));
	}
	
	public Button button(String text) {		
		return button(text,null);
	}
	
	public boolean debug(String tx, Object... fills) {
		LOG.debug(tx, fills);
		return false;
	}

	public static String distance(long l) {
		String unit = Plan.lengthUnit;
		if (DEFAULT_LENGTH_UNIT.equals(unit)) {
			double d = l;
			if (l > 1_000_000) {
				d = l/1_000_000d;
				unit = t("km");
			} else
			if (l > 1_000) {
				d = l/1_000;
				unit = t("m");
			} else
			if (l > 10) {
				d = l/10;
				unit = t("cm");
			}
			return String.format("%.3f", d)+NBSP+unit;
		}
		return l+NBSP+unit;
	}
		
	public static Form form(String id,List<Map.Entry<String, Tag>> elements) {
		Form form = new Form(id);

		Table table = new Table();
		for (Map.Entry<String, Tag>entry : elements) {
			String key = entry.getKey();	
			Tag val = entry.getValue();
			if (isNull(key) && val instanceof Input) ((Input)val).hideIn(form);
			table.addRow(isSet(key)?key:"",entry.getValue());
		}

		table.addTo(form);
		
		new Button(t("Apply"),form).addTo(form);
		return form;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends BaseClass> T get(Id id) {
		if (isNull(id)) return null;
		return (T) registry.get(id);
	}
	
	public Id id() {
		if (isNull(id)) id = new Id();
		return id;
	}
	
	public static boolean isNull(Object o) {
		return o==null;
	}

	public static boolean isSet(Object...o) {
		for (Object x : o) {
			if (x == null) return false;
		}
		return true;
	}
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		if (isSet(id)) json.put(ID, id().toString());
		if (isSet(notes) && !notes.isEmpty()) json.put(NOTES, notes);
		Set<String> customFieldNames = BaseClass.customFieldNames.get(getClass());
		JSONObject customFields = null;
		if (isSet(customFieldNames)) for (String fieldName : customFieldNames){
			String val = customFieldValues.get(fieldName);
			if (isSet(val) && !val.trim().isEmpty()) {
				if (isNull(customFields)) customFields = new JSONObject();
				customFields.put(fieldName, val);
			}
		}
		if (isSet(customFields)) json.put(CUSTOM_FIELDS, customFields);
		
		return json;
	}
	
	public Tag link(String tagClass,Object caption) {
		String highlightId = (caption instanceof BaseClass) ? ((BaseClass)caption).id().toString() : null;
		return link(tagClass,caption,highlightId);
	}

	public Tag link(String tagClass,Object caption, String highlightId) {
		return link(tagClass,caption,null,highlightId);
	}

	
	public Tag link(String tagClass,Object caption,Map<String,String> additionalProps,String highlightId) {
		Tag link = link(tagClass,caption.toString(),props(additionalProps));
		if (isSet(highlightId))	link.attr("onmouseover", "highlight('"+highlightId+"',true);").attr("onmouseout", "highlight('"+highlightId+"',false);");

		if (isSet(notes) && !notes.isEmpty()) link.title(notes);
		return link;
	}
	
	public static Tag link(String tagClass,String caption,Map<String,String> props) {
		String json = new JSONObject(props).toString().replace("\"", "'");
		return new Tag(tagClass)
			.clazz("link")
			.attr("onclick","request("+json+");")
			.content(caption.toString());
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends BaseClass> List<T> listElements(Class<T> cls) {
		ArrayList<T> result = new ArrayList<T>();
		for (BaseClass object : registry.values()) {
			if (isSet(object) && cls.isAssignableFrom(object.getClass())) {
				result.add((T) object);
			}
		}
		return result;
	}
	
	public BaseClass load(JSONObject json) {
		if (json.has(ID)) {
			id = Id.from(json);
			register();
		}
		if (json.has(NOTES)) notes = json.getString(NOTES);
		if (json.has(CUSTOM_FIELDS)) {
			JSONObject customFields = json.getJSONObject(CUSTOM_FIELDS);
			for (String fieldName : customFields.keySet()) {
				String val = customFields.getString(fieldName);
				Set<String> customFieldNames = BaseClass.customFieldNames.get(getClass());
				if (isNull(customFieldNames)) {
					customFieldNames = new HashSet<String>();
					BaseClass.customFieldNames.put(getClass(),customFieldNames);
				}
				customFieldNames.add(fieldName);
				customFieldValues.put(fieldName, val);
			}
		}
		return this;
	}
	
	public static String md5sum(Object o) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(o.toString().getBytes(UTF8));
			StringBuffer sb = new StringBuffer();
			for (byte b : digest.digest()) {
			    sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
			    sb.append(HEX_CHARS[b & 0x0F]);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	public static HashMap<String, String> merged(Map<String, String> base, Map<String, String> overlay) {
		HashMap<String,String> merged = new HashMap<>(base);
		overlay.entrySet().stream().forEach(entry -> merged.put(entry.getKey(), entry.getValue()));
		return merged;
	}
	
	public BaseClass parent() {
		return parent;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BaseClass> T parent(BaseClass parent) {
		this.parent = parent;
		return (T) this;
	}
	
	public Window properties(String...error) {
		return properties(new ArrayList<>(), new FormInput(), new ArrayList<>(),error);
	}

	
	protected Window properties(List<Fieldset> preForm,FormInput formInputs,List<Fieldset> postForm, String...errorMessages) {
		Window win = new Window(getClass().getSimpleName()+"-properties", t("Properties of {}",this.title()));

		Tag errorDiv = new Tag("div").clazz("error").content("");
		if (errorMessages != null && errorMessages.length > 0) {
			for (String errorMessage : errorMessages) {
				if (isSet(errorMessage)) new Tag("p").content(errorMessage).addTo(errorDiv);
			}
		}
		errorDiv.addTo(win);
		
		preForm.forEach(fieldset -> fieldset.addTo(win));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ACTION, ACTION_UPDATE)));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(REALM,realm())));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ID,id())));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(t("Notes"),new TextArea(NOTES,notes)));

		form(getClass().getSimpleName()+"-prop-form",formInputs)
			.addTo(new Fieldset(t("Basic properties")).id(PROPS_BASIC))
			.addTo(win);
		
		postForm.forEach(fieldset -> fieldset.addTo(win));
		
		Fieldset customFields = new Fieldset(t("custom fields")).id("props-custom");
		
		Form customForm = new Form(CUSTOM_FIELDS);
		new Input(ACTION, ACTION_UPDATE).hideIn(customForm);
		new Input(REALM,realm()).hideIn(customForm);
		new Input(ID,id()).hideIn(customForm);
		
		Table table = new Table();
		
		Set<String> fieldNames = customFieldNames.get(getClass());
		if (isSet(fieldNames)) for (String fieldName : fieldNames) {
			String val = customFieldValues.get(fieldName);
			table.addRow(fieldName,new Input(fieldName,isNull(val) ? "" : val));
		}

		table.addRow(t("Add new custom field"),new Input(NEW_CUSTOM_FIELD_NAME));
		table.addTo(customForm);
		new Button(t("Apply"),customForm).addTo(customForm).addTo(customFields);
		customFields.addTo(win);		
		
		Fieldset history = new Fieldset(t("History")).id("history");
		
		Form form = new Form("add-history-entry");
		new Input(REALM, REALM_HISTORY).hideIn(form);
		new Input(ACTION, ACTION_ADD).hideIn(form);
		new Input(ID,id()).hideIn(form);
		new TextArea(NOTES).addTo(form);
		new Button(t("Add entry"), form).addTo(form);
		form.addTo(history);
		
		table = new Table();
		table.addHead(t("Date/Time"),t("Event"),t("Actions"));
		for (LogEntry entry : History.getFor(this)) table.addRow(
			new SimpleDateFormat("YYYY-dd-MM HH:mm").format(entry.date()),
			entry.getText(),
			button(t("delete"),Map.of(REALM,REALM_HISTORY,ACTION,ACTION_DROP,TIME,""+entry.getTime())));
		table.addTo(history).addTo(win);
		
		return win;
	}
	
	public String realm() {
		if (this instanceof Contact) return REALM_CONTACT;
		if (this instanceof Decoder) return REALM_DECODER;
		if (this instanceof Tile) return REALM_PLAN;
		if (this instanceof Function) return REALM_FUNCTION;
		if (this instanceof Locomotive) return REALM_LOCO;
		if (this instanceof Car) return REALM_CAR;		
		if (this instanceof Action) return REALM_ACTIONS;
		if (this instanceof Condition) return REALM_CONDITION;
		if (this instanceof Route) return REALM_ROUTE;
		if (this instanceof Train) return REALM_TRAIN;
		return REALM_PLAN;
	}
	
	private String title() {
		return toString();
	}

	private Map<String,String> props(Map<String,String> additionalProps){
		HashMap<String,String> props = new HashMap<String, String>(Map.of(REALM, realm(), ACTION, ACTION_PROPS, ID, id().toString()));
		if (isSet(additionalProps)) props.putAll(additionalProps);
		return props;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BaseClass> T register() {
		registry.put(id(),this);
		return (T) this;
	}
	
	public BaseClass remove() {
		LOG.debug("BaseClass.Remove {} ({})",id(),this);
		if (isSet(parent)) parent.removeChild(this);
		return registry.remove(id());
	}
		
	protected void removeChild(BaseClass child) {}
	
	public static void resetRegistry() {
		registry = new HashMap<BaseClass.Id, BaseClass>();
		customFieldNames = new HashMap<Class<? extends BaseClass>, Set<String>>();
	}
	
	public static <T,L extends List<T>> L reverse(L list){		
		Collections.reverse(list);
		return list;
	}

	public void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String t(String txt, Object...fills) {
		if (isSet(fills)) for (int i=0; i<fills.length; i++) {
			if ("\\".equals(fills[i])) fills[i]="\\\\";
		}
		return Translation.get(Application.class, txt, fills);
	}
	
	public static String time(Date date) {
		return isSet(date) ? new SimpleDateFormat("YYYY-dd-MM HH:mm").format(date) : null;
	}

	
	public static long timestamp() {
		return new Date().getTime();
	}
	
	public BaseClass unregister() {
		return registry.remove(this.id());
	}

	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(NOTES)) notes = params.getString(NOTES).trim();
		String newCustomFieldName = params.getString(NEW_CUSTOM_FIELD_NAME);
		Set<String> fieldNames = customFieldNames.get(getClass());
		if (isSet(fieldNames)) for (String fieldName : fieldNames) {
			String fieldValue = params.getString(fieldName);
			if (isSet(fieldValue)) customFieldValues.put(fieldName, fieldValue);
		}
		if (isSet(newCustomFieldName) && !newCustomFieldName.trim().isEmpty()) {
			if (isNull(fieldNames)) {
				fieldNames = new HashSet<String>();
				customFieldNames.put(getClass(), fieldNames);
			}
			fieldNames.add(newCustomFieldName);
		}
		
		return this;
	}
}
