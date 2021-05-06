package de.srsoftware.web4rail.tiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.actions.AlterDirection;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

/**
 * Base class for all tiles
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Tile extends BaseClass implements Comparable<Tile> {

	protected static Logger LOG = LoggerFactory.getLogger(Tile.class);
	private static int DEFAUT_LENGTH = 100; // 10cm

	private static final String LENGTH = "length";
	private static final String ONEW_WAY = "one_way";
	private static final String POS = "pos";
	private static final String TYPE = "type";
	private static final String X = "x";
	private static final String Y = "y";

	private boolean isTrack = true;
	private boolean disabled;
	private int length = DEFAUT_LENGTH;
	protected Direction oneWay = null;
	private TreeSet<Route> routes = new TreeSet<>((r1, r2) -> r1.toString().compareTo(r2.toString()));
	private Train reservingTrain,lockingTrain,occupyingTrain;
	public Integer x = null;
	public Integer y = null;

	public void add(Route route) {
		this.routes.add(route);
	}

	protected HashSet<String> classes() {
		HashSet<String> classes = new HashSet<String>();
		classes.add("tile");
		classes.add(getClass().getSimpleName());
		if (isSet(reservingTrain)) classes.add(RESERVED);
		if (isSet(lockingTrain)) classes.add(LOCKED);
		if (isSet(occupyingTrain)) classes.add(OCCUPIED);
		return classes;
	}

	public Object click(boolean shift) throws IOException {
		LOG.debug("{}.click()", getClass().getSimpleName());
		if (!shift) {
			if (isSet(occupyingTrain)) return occupyingTrain.properties();	
			if (isSet(lockingTrain))   return lockingTrain.properties();
		}
		
		return properties();
	}

	@Override
	public int compareTo(Tile other) {
		if (x == other.x) return y - other.y;
		return x - other.x;
	}

	public JSONObject config() {
		return new JSONObject();
	}

	public Map<Connector, Turnout.State> connections(Direction from) {
		return new HashMap<>();
	}

	public boolean free(Train oldTrain) {
		if (isNull(oldTrain)) return false;
		boolean result = false;
		if (reservingTrain == oldTrain && (result = true)) reservingTrain = null;
		if (lockingTrain   == oldTrain && (result = true)) lockingTrain   = null;
		if (occupyingTrain == oldTrain && (result = true)) occupyingTrain = null;
		oldTrain.unTrace(this);
		if (result) plan.place(this);
		return result;
	}

	public int height() {
		return 1;
	}

	public Id id() {
		return Tile.id(x, y);
	}

	public static Id id(int x, int y) {
		return new Id(x + "-" + y);
	}

	private static void inflate(String clazz, JSONObject json, Plan plan) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		clazz = Tile.class.getName().replace(".Tile", "." + clazz);
		Tile tile = (Tile) Tile.class.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		tile.load(json).parent(plan).register();
		if (tile instanceof TileWithShadow) ((TileWithShadow) tile).placeShadows();
		plan.place(tile);
	}

	public boolean isDisabled() {
		return disabled;
	}
	
	protected boolean isFree() {
		if (isSet(lockingTrain)) return false;
		if (isSet(reservingTrain)) return false;
		if (isSet(occupyingTrain)) return false;
		return true;
	}

	public boolean isFreeFor(Context newTrain) {
		//LOG.debug("{}.isFreeFor({})", this, newTrain);
		if (isDisabled()) {
			LOG.debug("{} is disabled!", this);
			return false;
		}
		
		Train train = newTrain.train();
		
		if (isSet(reservingTrain) && reservingTrain != train) {
			LOG.debug("{} is reserved for {}",this,reservingTrain);
			return false; // nicht in reservierten Block einfahren!
		}

		if (isSet(lockingTrain) && lockingTrain != train) {
			LOG.debug("{} is locked for {}",this,lockingTrain);
			return false; // nicht in reservierten Block einfahren!
		}

		if (isSet(occupyingTrain) && occupyingTrain != train) {
			LOG.debug("{} is occupied by {}",this,occupyingTrain);
			return isSet(train) && train.isShunting(); // nur in belegte Blöcke einfahren, wenn Rangiermodus aktiv!
		}

		return true;
	}
	
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TYPE, getClass().getSimpleName());
		if (isSet(x) && isSet(y)) json.put(POS, new JSONObject(Map.of(X, x, Y, y)));
		if (isSet(oneWay)) json.put(ONEW_WAY, oneWay);
		if (disabled) json.put(DISABLED, true);
		if (isSet(occupyingTrain)) json.put(REALM_TRAIN, occupyingTrain.id());
		json.put(LENGTH, length);
		return json;
	}

	public int length() {
		return length;
	}

	public Tile length(int newLength) {
		length = Math.max(0, newLength);
		return this;
	}

	/**
	 * If arguments are given, the first is taken as content, the second as tag
	 * type. If no content is supplied, id() is set as content. If no type is
	 * supplied, "span" is preset.
	 * 
	 * @param args
	 * @return
	 */
	public Tag link(String... args) {
		String tx = args.length < 1 ? id() + NBSP : args[0];
		String type = args.length < 2 ? "span" : args[1];
		return super.link(type, (Object) tx, Map.of(ACTION, ACTION_CLICK));
	}

	public static void load(Object object, Plan plan) {
		if (object instanceof JSONObject) {
			JSONObject json = (JSONObject) object;
			String clazz = json.getString(TYPE);
			if (clazz.equals("TurnTrain")) {
				clazz = AlterDirection.class.getSimpleName();
			}
			try {
				Tile.inflate(clazz, json, plan);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException
				| IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Tile load(JSONObject json) {
		if (json.has(ID)) json.remove(ID); // id should be created from coordinates
		super.load(json);
		if (json.has(POS)) {
			JSONObject pos = json.getJSONObject(POS);
			x = pos.getInt(X);
			y = pos.getInt(Y);
		}
		if (json.has(DISABLED)) disabled = json.getBoolean(DISABLED);
		if (json.has(LENGTH)) length = json.getInt(LENGTH);
		if (json.has(ONEW_WAY)) oneWay = Direction.valueOf(json.getString(ONEW_WAY));
		if (json.has(REALM_TRAIN))
		new LoadCallback() {
			
			@Override
			public void afterLoad() {
				occupyingTrain = Train.get(new Id(json.getString(REALM_TRAIN)));
			}
		};
		return this;
	}
	
	public boolean lockFor(Context context,boolean downgrade) {
		Train newTrain = context.train();
		LOG.debug("{}.lockFor({})",this,newTrain);
		if (isNull(newTrain)) return false;
		if (isSet(reservingTrain) && reservingTrain != newTrain) return debug("{} already reserved for  {}",this,reservingTrain);
		if (isSet(lockingTrain)) {
			if (lockingTrain != newTrain) return debug("{} already locked by {}",this,lockingTrain);
			return true; // already locked!
		}
		if (isSet(occupyingTrain)) {
			if (occupyingTrain != newTrain && !newTrain.isShunting()) return debug("{} already occupied by {}",this,occupyingTrain);
			lockingTrain = newTrain;
			if (!downgrade) return true;
		}
		lockingTrain = newTrain;
		reservingTrain = occupyingTrain = null;		
		plan.place(this);
		return true;
	}
	
	public Train lockingTrain() {
		if (isSet(lockingTrain)) return lockingTrain;
		if (isSet(occupyingTrain)) return occupyingTrain;
		return null;
	}
	
	public boolean move(int dx, int dy) {
		int destX = x + (dx > 0 ? width() : dx);
		int destY = y + (dy > 0 ? height() : dy);
		if (destX < 0 || destY < 0) return false;

		Tile tileAtDestination = plan.get(id(destX, destY), true);
		if (isSet(tileAtDestination) && !tileAtDestination.move(dx, dy)) return false;
		plan.drop(this);
		position(x + dx, y + dy);
		plan.place(this);
		return true;
	}

	protected void noTrack() {
		isTrack = false;
	}
	
	public Train occupyingTrain() {
		return occupyingTrain;
	}

	public Tile position(int x, int y) {
		this.x = x;
		this.y = y;
		return this;
	}

	public List<Direction> possibleDirections() {
		return new Vector<Plan.Direction>();
	}

	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm, String... errors) {
		Fieldset fieldset = null;

		if (isSet(occupyingTrain)) {
			fieldset = new Fieldset(t("Train"));
			occupyingTrain.link("span", t("Train") + ":" + NBSP + occupyingTrain + NBSP).addTo(fieldset);
			if (isSet(occupyingTrain.route())) {
				occupyingTrain.button(t("stop"), Map.of(ACTION, ACTION_STOP)).addTo(fieldset);
			} else {
				occupyingTrain.button(t("depart"), Map.of(ACTION, ACTION_START)).addTo(fieldset);
			}
			if (occupyingTrain.usesAutopilot()) {
				occupyingTrain.button(t("quit autopilot"), Map.of(ACTION, ACTION_QUIT)).addTo(fieldset);
			} else {
				occupyingTrain.button(t("auto"), Map.of(ACTION, ACTION_AUTO)).addTo(fieldset);
			}
		}

		if (isSet(fieldset)) preForm.add(fieldset);

		if (isTrack) {
			formInputs.add(t("Length"), new Input(LENGTH, length).numeric().addTo(new Tag("span")).content(NBSP + lengthUnit));
			Checkbox checkbox = new Checkbox(DISABLED, t("disabled"), disabled);
			if (disabled) checkbox.clazz("disabled");
			formInputs.add(t("State"), checkbox);
		}
		
		if (isSet(lockingTrain())) formInputs.add(t("Locked by {}",lockingTrain()), button(t("free"), Map.of(REALM,REALM_PLAN,ACTION,ACTION_FREE)));

		List<Direction> pd = possibleDirections();
		if (!pd.isEmpty()) {
			Tag div = new Tag("div");
			new Radio("oneway", "none", t("No"), isNull(oneWay)).addTo(div);
			for (Direction d : pd) {
				new Radio("oneway", d.toString(), t(d.toString()), d == oneWay).addTo(div);
			}
			formInputs.add(t("One way"), div);
		}

		if (!routes.isEmpty()) {
			fieldset = new Fieldset(t("Routes")).id("props-routes");
			Tag routeList = new Tag("ol");
			boolean empty = true;
			for (Route route : routes) {
				if (route.isDisabled()) continue;
				Tag li = route
					.link("span", route.name() + (route.isDisabled() ? " [" + t("disabled") + "]" : "") + NBSP)
					.addTo(new Tag("li").clazz("link"));
				route.button(t("delete route"), Map.of(ACTION, ACTION_DROP)).addTo(li);
				button(t("simplify name"), Map.of(ACTION, ACTION_AUTO, ROUTE, route.id().toString())).addTo(li);
				li.addTo(routeList);
				empty = false;
			}
			if (!empty) {
				new Tag("h4").content(t("Routes using this tile")).addTo(fieldset);
				routeList.addTo(fieldset);
				postForm.add(fieldset);
			}

			routeList = new Tag("ol");
			empty = true;
			for (Route route : routes) {
				if (!route.isDisabled()) continue;
				Tag li = route
					.link("span", route.name() + (route.isDisabled() ? " [" + t("disabled") + "]" : "") + NBSP)
					.addTo(new Tag("li").clazz("link"));
				route.button(t("delete route"), Map.of(ACTION, ACTION_DROP)).addTo(li);
				button(t("simplify name"), Map.of(ACTION, ACTION_AUTO, ROUTE, route.id().toString())).addTo(li);
				li.addTo(routeList);
				empty = false;
			}
			if (!empty) {
				new Tag("h4").content(t("disabled routes")).addTo(fieldset);
				routeList.addTo(fieldset);
			}
		}

		return super.properties(preForm, formInputs, postForm, errors);
	}
	
	@Override
	public BaseClass remove() {
		while (!routes.isEmpty()) routes.first().remove();
		return super.remove();
	}
	
	@Override
	public void removeChild(BaseClass child) {
		String childAsString = child.toString();
		if (childAsString.length() > 20) childAsString = childAsString.substring(0, 20) + "…";
		LOG.debug("Removing {} from {}", childAsString, this);
		if (child instanceof Route) routes.remove(child);

		if (child == reservingTrain) reservingTrain = null;
		if (child == lockingTrain) lockingTrain = null;
		if (child == occupyingTrain) occupyingTrain = null;
		super.removeChild(child);
		plan.place(this);
	}
	
	private static String replace(String line, Entry<String, Object> replacement) {
		String key = replacement.getKey();
		Object val = replacement.getValue();
		int start = line.indexOf(key);
		int len = key.length();
		while (start > 0) {
			int end = line.indexOf("\"", start);
			int end2 = line.indexOf("<", start);
			if (end2 > 0 && (end < 0 || end2 < end)) end = end2;
			String tag = line.substring(start, end);
			if (tag.length() > len) val = Integer.parseInt(tag.substring(len)) + (int) val;
			line = line.replace(tag, "" + val);
			start = line.indexOf(key);
		}
		return line;
	}

	public boolean reserveFor(Context context) {
		Train newTrain = context.train();
		LOG.debug("{}.reserverFor({})",this,newTrain);
		if (isNull(newTrain)) return false;
		if (isSet(reservingTrain)) {
			if (reservingTrain != newTrain) return debug("{} already reserved for  {}",this,reservingTrain);
			return true; // already reserved for newTrain
		}
		if (isSet(lockingTrain)) {
			if (lockingTrain != newTrain) return debug("{} already locked by {}",this,lockingTrain);
			return true; // do not downgrade!
		}
		if (isSet(occupyingTrain)) {
			if (occupyingTrain != newTrain && !newTrain.isShunting()) return debug("{} already occupied by {}",this,occupyingTrain);
			return true; // do not downgrade!
		}		
		reservingTrain = newTrain;
		plan.place(this);
		return true;
	}
	
	protected Train reservingTrain() {
		return reservingTrain;
	}

	public TreeSet<Route> routes() {
		return routes;
	}

	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Tile tile : BaseClass.listElements(Tile.class)) {
			if (isNull(tile) || tile instanceof Shadow || tile instanceof BlockContact) continue;
			file.append(tile.json() + "\n");
		}
		file.close();
	}

	
	public void setEnabled(boolean enabled) {
		boolean show = (disabled == enabled);
		disabled = !enabled;
		if (show) plan.place(this);
	}
	
	public boolean setTrain(Train newTrain) {
		if (disabled) return false;
		if (isNull(newTrain)) return false;		
		if (isSet(reservingTrain) && newTrain != reservingTrain) return false;
		if (isSet(lockingTrain)   && newTrain != lockingTrain)   return false;
		if (isSet(occupyingTrain) && (newTrain != occupyingTrain) && !newTrain.isShunting()) return false;
		reservingTrain = lockingTrain = null;
		if (occupyingTrain == newTrain) return true;
		occupyingTrain = newTrain;
		plan.place(this);
		return true;
	}

	public Tag tag(Map<String, Object> replacements) throws IOException {
		int width = 100 * width();
		int height = 100 * height();
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%width%", width);
		replacements.put("%height%", height);
		String style = "";
		Tag svg = new Tag("svg").id(isSet(x) && isSet(y) ? id().toString() : getClass().getSimpleName())
			.clazz(classes()).size(100, 100).attr("name", getClass().getSimpleName())
			.attr("viewbox", "0 0 " + width + " " + height);
		if (isSet(x)) style = "left: " + (30 * x) + "px; top: " + (30 * y) + "px;";
		if (width() > 1) style += " width: " + (30 * width()) + "px;";
		if (height() > 1) style += " height: " + (30 * height()) + "px;";

		if (!style.isEmpty()) svg.style(style);

		File file = new File(System.getProperty("user.dir") + "/resources/svg/" + getClass().getSimpleName() + ".svg");
		if (file.exists()) {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			StringBuffer sb = new StringBuffer();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("<svg") || line.endsWith("svg>")) continue;
				for (Entry<String, Object> replacement : replacements.entrySet()) line = replace(line, replacement);
				sb.append(line + "\n");
			}
			scanner.close();
			svg.content(sb.toString());

			if (isSet(oneWay)) {
				switch (oneWay) {
					case EAST:
						new Tag("polygon").clazz("oneway").attr("points", "100,50 75,35 75,65").addTo(svg);
						break;
					case WEST:
						new Tag("polygon").clazz("oneway").attr("points", "0,50 25,35 25,65").addTo(svg);
						break;
					case SOUTH:
						new Tag("polygon").clazz("oneway").attr("points", "50,100 35,75 65,75").addTo(svg);
						break;
					case NORTH:
						new Tag("polygon").clazz("oneway").attr("points", "50,0 35,25 65,25").addTo(svg);
						break;
					default:
				}
			}
			String title = title();
			if (isSet(title)) new Tag("title").content(title()).addTo(svg);
		} else {
			new Tag("title").content(t("No display defined for this tile ({})", getClass().getSimpleName())).addTo(svg);
			new Tag("text").pos(35, 70).content("?").addTo(svg);
		}

		return svg;
	}

	public String title() {
		return getClass().getSimpleName() + " @ (" + x + ", " + y + ")";
	}

	@Override
	public String toString() {
		return t("{}({},{})", getClass().getSimpleName(), x, y);
	}

	public Tile update(HashMap<String, String> params) {
		LOG.debug("{}.update({})", getClass().getSimpleName(), params);
		String oneWayDir = params.get("oneway");
		if (isSet(oneWayDir)) {
			try {
				oneWay = Direction.valueOf(oneWayDir);
			} catch (Exception e) {
				oneWay = null;
			}
		}
		disabled = "on".equals(params.get(DISABLED));

		String len = params.get(LENGTH);
		if (isSet(len)) length(Integer.parseInt(len));
		super.update(params);
		plan.place(this);
		return this;
	}

	public int width() {
		return 1;
	}
}

