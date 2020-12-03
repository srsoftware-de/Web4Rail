package de.srsoftware.web4rail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
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
	
	public static final Logger LOG = LoggerFactory.getLogger(BaseClass.class);
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
		
		public Context(BaseClass object) {
			main = object;
			if (main instanceof Tile) this.tile = (Tile) main;
			if (main instanceof Contact) this.contact = (Contact) main;
			if (main instanceof Block) this.block = (Block) main;
			if (main instanceof Train) this.train = (Train) main;
			if (main instanceof Route) this.route = (Route) main;
			if (main instanceof Action) this.action = (Action) main;
			if (main instanceof Condition) this.condition = (Condition) main;
			if (main instanceof Car) this.car = (Car) main;
		}
		
		public Action action() {
			return action;
		}
		
		public Block block() {
			return block;
		}
		
		public Context block(Block newBlock) {
			block = newBlock;
			return this;
		}

		public Car car() {
			return car;
		}
		
		public Context clone() {
			return new Context(main);
		}
		
		public Condition condition() {
			return condition;
		}
		
		public Contact contact() {
			return contact;
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
			sb.append(t("Train: {}",train));
			if (isSet(route))   sb.append(", "+t("Route: {}",route));
			if (isSet(contact)) sb.append(", "+t("Contact: {}",contact));
			sb.append(")");
			return sb.toString();
		}
		
		public Train train() {
			return train;
		}

		public Context train(Train newTrain) {
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
	
	public String realm() {
		if (this instanceof Tile) return REALM_PLAN;
		if (this instanceof Contact) return REALM_CONTACT;

		if (this instanceof Car) return REALM_CAR;
		if (this instanceof Locomotive) return REALM_LOCO;
		
		if (this instanceof Action) return REALM_ACTIONS;
		if (this instanceof Condition) return REALM_CONDITION;
		if (this instanceof Route) return REALM_ROUTE;
		if (this instanceof Train) return REALM_TRAIN;
		return REALM_PLAN;
	}
	
	public Map<String,String> contextAction(String action){
		return Map.of(ACTION,action,CONTEXT,realm()+":"+id());
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
	public static <T> T get(Id id) {
		BaseClass element = registry.get(id);
		if (isNull(element)) return null;
		try {
			return (T) element;
		} catch (ClassCastException e) {
			return null;
		}		
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
		return json;
	}
	
	public Tag link(String tagClass,Object caption) {
		return link(tagClass,caption,null);
	}

	public Tag link(String tagClass,Object caption,Map<String,String> additionalProps) {
		String json = new JSONObject(props(additionalProps)).toString().replace("\"", "'");
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
		Window win = new Window(getClass().getSimpleName()+"-properties", t("Properties of {}",this));
		
		preForm.forEach(fieldset -> fieldset.addTo(win));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ACTION, ACTION_UPDATE)));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(REALM,realm())));
		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(null,new Input(ID,id())));

		formInputs.add(new AbstractMap.SimpleEntry<String, Tag>(t("Notes"),new TextArea(NOTES,notes)));

		form(getClass().getSimpleName()+"-prop-form",formInputs)
			.addTo(new Fieldset(t("Basic properties")))
			.addTo(win);
		
		postForm.forEach(fieldset -> fieldset.addTo(win));
		
		return win;
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
		if (isSet(parent)) parent.removeChild(this);
		return registry.remove(id());
	}
		
	protected abstract void removeChild(BaseClass child);

	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		if (params.containsKey(NOTES)) notes = params.get(NOTES).trim();
		return this;
	}
}
