package de.srsoftware.web4rail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
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

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Table;
import de.srsoftware.web4rail.tags.TextArea;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Tile;

public abstract class BaseClass implements Constants{
	protected static Plan plan; // the track layout in use
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
			LOG.debug("{}.block({})",this,newBlock);
			block = newBlock;
			return this;
		}

		public Car car() {
			return car;
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
			return clone;
		}
		
		public Condition condition() {
			return condition;
		}
		
		public Contact contact() {
			return contact;
		}
		
		public void contact(Contact newContact) {
			contact = newContact;
		}

		
		public Direction direction() {
			return direction;
		}

		public Context direction(Direction newDirection) {
			LOG.debug("{}.direction({})",this,newDirection);
			direction = newDirection;
			return this;
		}
		
		public boolean invalidated() {
			return isNull(main);
		}
		
		public Context setMain(BaseClass object) {
			main = object;
			LOG.debug("{}.setMain({})",this,object);
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
			if (isSet(route))   sb.append(", "+t("Route: {}",route));
			if (isSet(contact)) sb.append(", "+t("Contact: {}",contact));
			sb.append(")");
			return sb.toString();
		}
		
		public Train train() {
			return train;
		}

		public Context train(Train newTrain) {
			LOG.debug("{}.train({})",this,newTrain);
			train = newTrain;
			return this;
		}
	}
	
	public class FormInput extends ArrayList<Map.Entry<String, Tag>>{
		
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
		
		public static Id from(Map<String,String> params) {
			return Id.from(params,ID);
		}


		public static Id from(Map<String, String> params, String key) {
			String sid = params.get(key);
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
	
	public Button button(String text,Map<String,String> additionalProps) {
		return new Button(text,props(additionalProps));
	}
	
	public Button button(String text) {		
		return button(text,null);
	}
		
	public Form form(String id,List<Map.Entry<String, Tag>> elements) {
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
		return (T) registry.get(id);
	}
	
	public Id id() {
		if (isNull(id)) id = new Id();
		return id;
	}
	
	public static boolean isNull(Object o) {
		return o==null;
	}

	public static boolean isSet(Object o) {
		return o != null;
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
		return link(tagClass,caption,null);
	}

	public Tag link(String tagClass,Object caption,Map<String,String> additionalProps) {
		return link(tagClass,caption.toString(),props(additionalProps));
	}
	
	public static Tag link(String tagClass,String caption,Map<String,String> props) {
		String json = new JSONObject(props).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(caption.toString());
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
	
	public BaseClass parent(BaseClass parent) {
		this.parent = parent;
		return this;
	}
	
	public Window properties() {
		return properties(new ArrayList<>(), new FormInput(), new ArrayList<>());
	}

	
	protected Window properties(List<Fieldset> preForm,FormInput formInputs,List<Fieldset> postForm) {
		Window win = new Window(getClass().getSimpleName()+"-properties", t("Properties of {}",this.title()));
		
		preForm.forEach(fieldset -> fieldset.addTo(win));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ACTION, ACTION_UPDATE)));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(REALM,realm())));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ID,id())));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(t("Notes"),new TextArea(NOTES,notes)));

		form(getClass().getSimpleName()+"-prop-form",formInputs)
			.addTo(new Fieldset(t("Basic properties")))
			.addTo(win);
		
		postForm.forEach(fieldset -> fieldset.addTo(win));
		
		Fieldset customFields = new Fieldset(t("custom fields"));
		
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
		
		return win;
	}
	
	public String realm() {
		if (this instanceof Contact) return REALM_CONTACT;
		if (this instanceof Tile) return REALM_PLAN;

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
	
	public BaseClass register() {
		registry.put(id(),this);
		return this;
	}
	
	public BaseClass remove() {
		LOG.debug("BaseClass.Remove {} ({})",id(),this);
		if (isSet(parent)) parent.removeChild(this);
		return registry.remove(id());
	}
		
	protected void removeChild(BaseClass child) {}

	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}
	
	public BaseClass unregister() {
		return registry.remove(this.id());
	}

	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(NOTES)) notes = params.get(NOTES).trim();
		String newCustomFieldName = params.get(NEW_CUSTOM_FIELD_NAME);
		Set<String> fieldNames = customFieldNames.get(getClass());
		if (isSet(fieldNames)) for (String fieldName : fieldNames) {
			String fieldValue = params.get(fieldName);
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
