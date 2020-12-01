package de.srsoftware.web4rail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.actions.Action;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
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
	
	public static class Id implements CharSequence, Comparable<Id>{
		private String internalId;

		public Id() {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			internalId = md5sum(new Date());
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
	
	public Map<String,String> contextAction(String action){
		String realm = REALM_PLAN;
		if (this instanceof Tile) realm = REALM_PLAN;
		if (this instanceof Contact) realm = REALM_CONTACT;

		if (this instanceof Car) realm = REALM_CAR;
		if (this instanceof Locomotive) realm = REALM_LOCO;
		
		if (this instanceof Action) realm = REALM_ACTIONS;
		if (this instanceof Condition) realm = REALM_CONDITION;
		if (this instanceof Route) realm = REALM_ROUTE;
		if (this instanceof Train) realm = REALM_TRAIN;

		return Map.of(ACTION,action,CONTEXT,realm+":"+id());
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
		return new JSONObject().put(ID, id().toString());
	}
	
	public Tag link(String tagClass,Object caption) {
		return link(tagClass,caption,null);
	}

	public Tag link(String tagClass,Object caption,Map<String,String> additionalProps) {
		String json = new JSONObject(props(additionalProps)).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(caption.toString());
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
	
	public Window properties() {
		return new Window(getClass().getSimpleName()+"-properties", t("Properties of {}",this));
	}
	
	public Map<String,String> props(Map<String,String> additionalProps){
		String realm = null;
		if (this instanceof Tile) realm = REALM_PLAN;
		if (this instanceof Contact) realm = REALM_CONTACT;

		if (this instanceof Car) realm = REALM_CAR;
		if (this instanceof Locomotive) realm = REALM_LOCO;
		
		if (this instanceof Train) realm = REALM_TRAIN;
		if (this instanceof Route) realm = REALM_ROUTE;
		if (this instanceof Action) realm = REALM_ACTIONS;
		if (this instanceof Condition) realm = REALM_CONDITION;
		
		HashMap<String,String> props = new HashMap<String, String>(Map.of(REALM, realm, ACTION, ACTION_PROPS, ID, id().toString()));
		if (isSet(additionalProps)) props.putAll(additionalProps);
		return props;
	}
		
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}
}
