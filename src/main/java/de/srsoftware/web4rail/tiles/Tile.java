package de.srsoftware.web4rail.tiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;

/**
 * Base class for all tiles
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Tile extends BaseClass implements Comparable<Tile>{
	protected static Logger LOG = LoggerFactory.getLogger(Tile.class);	
	private static int DEFAUT_LENGTH = 100; // 10cm
	
	private   static final String LENGTH     = "length";
	private   static final String LOCKED     = "locked";
	private   static final String OCCUPIED   = "occupied";
	private   static final String ONEW_WAY   = "one_way";
	private   static final String POS        = "pos";
	private   static final String ROUTE      = "route";
	private   static final String TYPE       = "type";
	private   static final String X          = "x";
	private   static final String Y          = "y";
	
	private   boolean         disabled  = false;
	private   boolean         isTrack   = true;
	private   int             length    = DEFAUT_LENGTH;
	protected Direction       oneWay    = null;
	protected Route           route     = null;
	private   TreeSet<Route>  routes    = new TreeSet<>();
	protected TreeSet<Shadow> shadows   = new TreeSet<>();
	protected Train           train     = null;	
	public    Integer         x         = null;
	public    Integer         y         = null;
	
	public void add(Route route) {
		this.routes.add(route);
	}

	public void addShadow(Shadow shadow) {
		shadows.add(shadow);
	}
	
	protected Vector<String> classes(){
		Vector<String> classes = new Vector<String>();
		classes.add("tile");
		classes.add(getClass().getSimpleName());
		if (isSet(route)) classes.add(LOCKED);
		if (isSet(train)) classes.add(OCCUPIED);
		if (disabled)     classes.add(DISABLED);
		return classes;
	}

	public Object click() throws IOException {
		LOG.debug("{}.click()",getClass().getSimpleName());
		return properties();
	}
	
	@Override
	public int compareTo(Tile other) {
		if (x == other.x) return y-other.y;
		return x - other.x;
	}
	
	public JSONObject config() {
		return new JSONObject();
	}
	
	public Map<Connector,Turnout.State> connections(Direction from){
		return new HashMap<>();
	}

	public int height() {
		return 1;
	}

	public Id id() {
		return Tile.id(x, y);
	}
	
	public static Id id(int x, int y) {
		return new Id(x+"-"+y);
	}
	
	private static void inflate(String clazz, JSONObject json, Plan plan) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		clazz = Tile.class.getName().replace(".Tile", "."+clazz);
		Tile tile = (Tile) Tile.class.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		tile.load(json).parent(plan);
		plan.set(tile.x, tile.y, tile);
	}

	public boolean isFreeFor(Train newTrain) {
		if (disabled) return false;
		if (isSet(route) && route.train != newTrain) return false;
		if (isSet(train) && train != newTrain) return false;
		return true;
	}
		
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(TYPE, getClass().getSimpleName());
		JSONObject pos = new JSONObject(Map.of(X,x,Y,y));
		json.put(POS, pos);
		if (isSet(route))     json.put(ROUTE, route.id());
		if (isSet(oneWay))    json.put(ONEW_WAY, oneWay);
		if (disabled)         json.put(DISABLED, true);
		if (isSet(train))     json.put(REALM_TRAIN, train.id());
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
	 * If arguments are given, the first is taken as content, the second as tag type.
	 * If no content is supplied, id() is set as content.
	 * If no type is supplied, "span" is preset.
	 * @param args
	 * @return
	 */
	public Tag link(String...args) {
		String tx = args.length<1 ? id()+NBSP : args[0];
		String type = args.length<2 ? "span" : args[1];
		return super.link(type, tx, Map.of(ACTION,ACTION_CLICK));
	}
	
	
	public static void load(Object object, Plan plan) {
		if (object instanceof JSONObject) {
			JSONObject json = (JSONObject) object;
			String clazz = json.getString(TYPE);
			try {
				Tile.inflate(clazz,json,plan);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}			
		}
	}

	public Tile load(JSONObject json) {
		super.load(json);
		JSONObject pos = json.getJSONObject(POS);
		x = pos.getInt(X);
		y = pos.getInt(Y);
		if (json.has(DISABLED))    disabled  = json.getBoolean(DISABLED);
		if (json.has(LENGTH))	   length    = json.getInt(LENGTH);
		if (json.has(ONEW_WAY))    oneWay    = Direction.valueOf(json.getString(ONEW_WAY));
		return this;
	}
	
	protected void noTrack() {
		isTrack  = false;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Fieldset fieldset = null;

		if (isSet(route)) {
			fieldset = new Fieldset(t("Route"));
			route.link("p",t("Locked by {}",route)).addTo(fieldset);
		}
		
		if (isSet(train)) {
			if (isSet(fieldset)) {
				fieldset.children().firstElement().content(" / "+t("Train"));
			} else fieldset = new Fieldset(t("Train"));
			train.link("span", t("Train")+":"+NBSP+train+NBSP).addTo(fieldset);
			if (isSet(train.route)) {
				train.button(t("stop"), Map.of(ACTION,ACTION_STOP)).addTo(fieldset);
			} else {
				train.button(t("start"), Map.of(ACTION,ACTION_START)).addTo(fieldset);
			}
			if (train.usesAutopilot()) {
				train.button(t("quit autopilot"), Map.of(ACTION,ACTION_QUIT)).addTo(fieldset);
			} else {
				train.button(t("auto"), Map.of(ACTION,ACTION_AUTO)).addTo(fieldset);
			}
		}
		
		if (isSet(fieldset)) preForm.add(fieldset);
		
		if (isTrack) {
			formInputs.add(t("Length"),new Input(LENGTH,length).numeric().addTo(new Tag("span")).content(NBSP+lengthUnit));
			formInputs.add(t("State"),new Checkbox(DISABLED, t("disabled"), disabled));
		}
		
		List<Direction> pd = possibleDirections();
		if (!pd.isEmpty()) {
			Tag div = new Tag("div");
			new Radio("oneway","none",t("No"),isNull(oneWay)).addTo(div);
			for (Direction d:pd) {
				new Radio("oneway",d.toString(),t(d.toString()),d == oneWay).addTo(div);
			}
			formInputs.add(t("One way"),div);
		}

		
		if (!routes.isEmpty()) {
			fieldset = new Fieldset(t("Routes using this tile"));
			Tag routeList = new Tag("ol");
			for (Route route : routes) {
				Tag li = route.link("span", route.name()+(route.isDisabled()?" ["+t("disabled")+"]" : "")+NBSP).addTo(new Tag("li").clazz("link"));
				route.button(t("delete route"),Map.of(ACTION,ACTION_DROP)).addTo(li);
				li.addTo(routeList);
			}
			routeList.addTo(fieldset);
			postForm.add(fieldset);
		}
		
		return super.properties(preForm, formInputs, postForm);
	}
	
	private static String replace(String line, Entry<String, Object> replacement) {
		String key = replacement.getKey();	
		Object val = replacement.getValue();
		int start = line.indexOf(key);
		int len = key.length();
		while (start>0) {
			int end = line.indexOf("\"",start);
			int end2 = line.indexOf("<",start);
			if (end2>0 && (end<0 || end2<end)) end=end2;
			String tag = line.substring(start, end);
			if (tag.length()>len) val = Integer.parseInt(tag.substring(len)) + (int) val;
			line = line.replace(tag, ""+val);
			start = line.indexOf(key);
		}
		return line;
	}

	public Route route() {
		return route;
	}
	
	public TreeSet<Route> routes() {
		return routes;
	}
	
	public static void saveAll(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Tile tile : BaseClass.listElements(Tile.class)) {
			if (isNull(tile) || tile instanceof Shadow) continue;
			file.append(tile.json()+"\n");
		}
		file.close();
	}
	
	public Tile set(Train newTrain) {
		if (newTrain == train) return this; // nothing to update
		this.train = newTrain;		
		return plan.place(this);
	}	

	public Tile setRoute(Route lockingRoute) {
		if (route == lockingRoute) return this; // nothing changed
		if (isSet(route) && isSet(lockingRoute)) throw new IllegalStateException(this.toString()); // tile already locked by other route
		route = lockingRoute;
		return plan.place(this);
	}

	public Tag tag(Map<String,Object> replacements) throws IOException {
		int width = 100*width();
		int height = 100*height();
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%width%",width);
		replacements.put("%height%",height);
		String style = "";
		Tag svg = new Tag("svg")
				.id(isSet(x) && isSet(y) ? id().toString() : getClass().getSimpleName())
				.clazz(classes())
				.size(100,100)
				.attr("name", getClass().getSimpleName())				
				.attr("viewbox", "0 0 "+width+" "+height);
				if (isSet(x)) style="left: "+(30*x)+"px; top: "+(30*y)+"px;";
				if (width()>1) style+=" width: "+(30*width())+"px;";
				if (height()>1) style+=" height: "+(30*height())+"px;";
		
		if (!style.isEmpty()) svg.style(style);

		File file = new File(System.getProperty("user.dir")+"/resources/svg/"+getClass().getSimpleName()+".svg");
		if (file.exists()) {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			StringBuffer sb = new StringBuffer();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("<svg") || line.endsWith("svg>")) continue;
				for (Entry<String, Object> replacement : replacements.entrySet()) line = replace(line,replacement);
				sb.append(line+"\n");
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
			new Tag("title").content(t("No display defined for this tile ({})",getClass().getSimpleName())).addTo(svg);
			new Tag("text")
				.pos(35,70)	
				.content("?")
				.addTo(svg);
		}

		return svg;
	}
	
	public String title() {
		return getClass().getSimpleName() + " @ ("+x+", "+y+")";
	}
	
	@Override
	public String toString() {
		return t("{}({},{})",getClass().getSimpleName(),x,y) ;
	}
	
	public Train train() {
		return train;
	}
	
	@Override
	public BaseClass remove() {
		super.remove();
		while (!routes.isEmpty()) routes.first().remove();
		while (!shadows.isEmpty()) shadows.first().remove();
		return this;
	}
	
	@Override
	public void removeChild(BaseClass child) {
		routes.remove(child);
		if (child instanceof Shadow) shadows.remove(child);
		if (child == train) train = null;
		if (child == route) route = null;
		plan.place(this);
	}
	
	public void unlock() {
		route = null;
		train = null;
		plan.place(this);
	}

	public Tile update(HashMap<String, String> params) {
		LOG.debug("{}.update({})",getClass().getSimpleName(),params);
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
		plan.place(this);
		return this;
	}
	
	public int width() {
		return 1;
	}
}
