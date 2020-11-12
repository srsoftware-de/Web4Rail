package de.srsoftware.web4rail.tiles;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
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
import java.util.Vector;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Application;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Checkbox;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;

/**
 * Base class for all tiles
 * @author Stephan Richter, SRSoftware
 *
 */
public abstract class Tile extends BaseClass{
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
	private   int             length    = DEFAUT_LENGTH;
	protected Direction       oneWay    = null;
	protected Plan            plan      = null;;
	protected Route           route     = null;
	private   HashSet<Route>  routes    = new HashSet<>();
	protected HashSet<Shadow> shadows   = new HashSet<>();
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
		return propMenu();
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

	public String id() {
		return Tile.id(x, y);
	}
	
	public static String id(int x, int y) {
		return x+"-"+y;
	}
	
	private static void inflate(String clazz, JSONObject json, Plan plan) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		clazz = Tile.class.getName().replace(".Tile", "."+clazz);
		Tile tile = (Tile) Tile.class.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
		tile.plan(plan);
		tile.load(json);
		plan.set(tile.x, tile.y, tile);
	}

	public boolean isFreeFor(Train newTrain) {
		if (disabled) return false;
		if (isSet(route)) return false;
		if (isSet(train) && newTrain != train) return false;
		return true;
	}
		
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(TYPE, getClass().getSimpleName());
		JSONObject pos = new JSONObject(Map.of(X,x,Y,y));
		json.put(POS, pos);
		if (isSet(route))     json.put(ROUTE, route.id());
		if (isSet(oneWay))    json.put(ONEW_WAY, oneWay);
		if (disabled)         json.put(DISABLED, true);
		if (isSet(train))     json.put(REALM_TRAIN, train.id);
		json.put(LENGTH, length);
		return json;
	}
	
	public int length() {
		return length;
	}
	
	public Tile length(int newLength) {
		length = newLength;
		return this;
	}
	
	public static void loadAll(String filename, Plan plan) throws IOException {		
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (isSet(line)) {
			JSONObject json = new JSONObject(line);
			String clazz = json.getString(TYPE);
			
			try {
				Tile.inflate(clazz,json,plan);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
				e.printStackTrace();
			}			
			
			line = file.readLine();
		}
		file.close();
	}

	protected Tile load(JSONObject json) throws IOException {
		JSONObject pos = json.getJSONObject(POS);
		x = pos.getInt(X);
		y = pos.getInt(Y);
		if (json.has(DISABLED))    disabled  = json.getBoolean(DISABLED);
		if (json.has(LENGTH))	   length    = json.getInt(LENGTH);
		if (json.has(ONEW_WAY))    oneWay    = Direction.valueOf(json.getString(ONEW_WAY));
		return this;
	}
		
	public Plan plan() {
		return plan;
	}
	
	public Tile plan(Plan plan) {
		this.plan = plan;
		return this;
	}

	public Tile position(int x, int y) {
		this.x = x;
		this.y = y;
		return this;
	}
	
	public List<Direction> possibleDirections() {
		return new Vector<Plan.Direction>();
	}
	
	public Form propForm(String formId) {
		Form form = new Form(formId);
		new Input(ACTION, ACTION_UPDATE).hideIn(form);
		new Input(REALM, REALM_PLAN).hideIn(form);
		new Input(ID,id()).hideIn(form);

		List<Direction> pd = possibleDirections();
		if (!pd.isEmpty()) {
			new Tag("h4").content(t("One way:")).addTo(form);
			new Radio("oneway","none",t("No"),isNull(oneWay)).addTo(form);		
			for (Direction d:pd) {
				new Radio("oneway",d.toString(),t(d.toString()),d == oneWay).addTo(form);
			}
		}
		return form;
	}
	
	public Window propMenu() {	
		Window window = new Window("tile-properties",t("Properties of {} @ ({},{})",title(),x,y));
		
		if (isSet(train)) {
			HashMap<String, Object> props = new HashMap<String,Object>(Map.of(REALM,REALM_TRAIN,ID,train.id));
			if (isSet(train.route)) {
				props.put(ACTION, ACTION_STOP);
				window.children().insertElementAt(new Button(t("stop"),props), 1);
			} else {
				props.put(ACTION, ACTION_START);
				window.children().insertElementAt(new Button(t("start"),props), 1);
			}
			window.children().insertElementAt(train.link("span"), 1);
			window.children().insertElementAt(new Tag("h4").content(t("Train:")), 1);
		}

		if (isSet(route)) link("p",Map.of(REALM,REALM_ROUTE,ID,route.id(),ACTION,ACTION_PROPS),t("Locked by {}",route)).addTo(window);

		Form form = propForm("tile-properties-"+id());
		new Tag("h4").content(t("Length")).addTo(form);
		new Input(LENGTH,length).numeric().addTo(new Label(t("Length")+":"+NBSP)).addTo(form);
		new Tag("h4").content(t("Availability")).addTo(form);
		new Checkbox(DISABLED, t("disabled"), disabled).addTo(form);
		new Button(t("Apply"),form).addTo(form);
		form.addTo(window);
		
		
		if (!routes.isEmpty()) {
			new Tag("h4").content(t("Routes using this tile:")).addTo(window);
			Tag routeList = new Tag("ol");
			for (Route route : routes) {
				String json = new JSONObject(Map.of(REALM,ROUTE,ID,route.id(),ACTION,ACTION_PROPS,CONTEXT,REALM_PLAN+":"+id())).toString().replace("\"", "'");
				Tag li = new Tag("span").attr("onclick","return request("+json+");").content(route.name()+(route.isDisabled()?" ["+t("disabled")+"]" : "")+NBSP).addTo(new Tag("li").clazz("link"));
				Map<String, Object> params = Map.of(REALM,REALM_ROUTE,ID,route.id(),ACTION,ACTION_DROP,Tile.class.getSimpleName(),id());
				new Button(t("delete route"),params).addTo(li);
				li.addTo(routeList);
			}
			routeList.addTo(window);
		}		
		return window;
	}
	
	public void remove(Route route) {
		routes.remove(route);
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
	
	public HashSet<Route> routes() {
		return routes;
	}
	
	public static void saveAll(HashMap<String, Tile> tiles ,String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Tile tile : tiles.values()) {
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

	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	public Tag tag(Map<String,Object> replacements) throws IOException {
		int width = 100*width();
		int height = 100*height();
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%width%",width);
		replacements.put("%height%",height);
		String style = "";
		Tag svg = new Tag("svg")
				.id(isSet(x) && isSet(y) ? id() : getClass().getSimpleName())
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
	
	public void unlock() {
		route = null;
		train = null;
		plan.place(this);
	}

	public Tile update(HashMap<String, String> params) throws IOException {
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
		plan.stream(tag(null).toString());
		return this;
	}
	
	public int width() {
		return 1;
	}
}
