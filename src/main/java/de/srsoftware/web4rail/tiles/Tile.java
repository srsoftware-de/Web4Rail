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
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Plan;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.Route;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;

public abstract class Tile {
	
	public static final String ID = "id";
	private static final String TYPE = "type";
	private static final String LOCKED = "locked";
	
	private static final String POS = "pos";
	private static final String X = "x";
	private static final String Y = "y";
	public int x = -1,y = -1;
	
	private static final String ROUTE = "route";
	protected Route route;
	
	private static final String OCCUPIED = "occupied";
	protected Train train;
	
	private static final String ONEW_WAY = "one_way";
	protected Direction oneWay = null;

	protected HashSet<Shadow> shadows = new HashSet<>();
	private HashSet<Route> routes = new HashSet<>();
	protected Plan plan;
	
	protected static Logger LOG = LoggerFactory.getLogger(Tile.class);	
	
	protected Vector<String> classes(){
		Vector<String> classes = new Vector<String>();
		classes.add("tile");
		classes.add(getClass().getSimpleName());
		if (route != null) classes.add(LOCKED);
		if (train != null) classes.add(OCCUPIED);
		return classes;
	}

	public void add(Route route) {
		this.routes.add(route);
	}

	public void addShadow(Shadow shadow) {
		shadows.add(shadow);
	}
	
	public Object click() throws IOException {
		return propMenu();
	}
	
	public JSONObject config() {
		return new JSONObject();
	}
	
	public Map<Connector,Turnout.State> connections(Direction from){
		return new HashMap<>();
	}

	public boolean free() {
		return route == null;
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
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(TYPE, getClass().getSimpleName());
		JSONObject pos = new JSONObject(Map.of(X,x,Y,y));
		json.put(POS, pos);
		if (route != null) json.put(ROUTE, route.id());
		if (oneWay != null) json.put(ONEW_WAY, oneWay);
		return json;
	}
	
	public int len() {
		return 1;
	}
	
	public static void loadAll(String filename, Plan plan) throws IOException {		
		BufferedReader file = new BufferedReader(new FileReader(filename));
		String line = file.readLine();
		while (line != null) {
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
		if (json.has(ONEW_WAY)) oneWay = Direction.valueOf(json.getString(ONEW_WAY));
		return this;
	}
	
	public void lock(Route route) throws IOException {
		this.route = route;
		plan.place(this);
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
	
	public Tag propForm() {
		Form form = new Form();
		new Input(Plan.ACTION, Plan.ACTION_UPDATE).hideIn(form);
		new Input(Plan.REALM, Plan.REALM_TILE).hideIn(form);
		new Input(Plan.ID,id()).hideIn(form);
		
		List<Direction> pd = possibleDirections();
		if (!pd.isEmpty()) {
			new Tag("h4").content(t("One way:")).addTo(form);
			new Radio("oneway","none",t("No"),oneWay == null).addTo(form);		
			for (Direction d:pd) {
				new Radio("oneway",d.toString(),t(d.toString()),d == oneWay).addTo(form);
			}
		}
		return form;
	}
	
	public Tag propMenu() {	
		Window window = new Window("tile-properties",t("Properties of {} @ ({},{})",getClass().getSimpleName(),x,y));
		Tag form = propForm();
		if (form!=null && form.children().size()>3) {
			new Button(t("save")).addTo(form);
			form.addTo(window);
		} else {
			window.content(t("This tile ({}) has no editable properties",getClass().getSimpleName()));
		}
		
		if (route != null) {
			new Tag("p").content(t("Locked by {}",route)).addTo(window);
		}
		
		if (!routes.isEmpty()) {
			new Tag("h4").content(t("Routes using this tile:")).addTo(window);
			Tag routeList = new Tag("ul");
			for (Route route : routes) {
				new Tag("li").clazz("link").attr("onclick","openRoute('"+route.id()+"')").content(route.name()).addTo(routeList);
			}
			routeList.addTo(window);
		}
		
		return window;
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
			if (tag.length()>len) {
				val = Integer.parseInt(tag.substring(len)) + (int) val;
			}
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
			if (tile == null || tile instanceof Shadow) continue;
			file.append(tile.json()+"\n");
		}
		file.close();
	}

	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	public Tag tag(Map<String,Object> replacements) throws IOException {
		int width = 100*len();
		int height = 100*height();
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%width%",width);
		replacements.put("%height%",height);
		String style = "";
		Tag svg = new Tag("svg")
				.id((x!=-1 && y!=-1)?(id()):(getClass().getSimpleName()))
				.clazz(classes())
				.size(100,100)
				.attr("name", getClass().getSimpleName())
				.attr("viewbox", "0 0 "+width+" "+height);
				if (x>-1) style="left: "+(30*x)+"px; top: "+(30*y)+"px;";
				if (len()>1) style+=" width: "+(30*len())+"px;";
				if (height()>1) style+=" height: "+(30*height())+"px;";
		
		if (!style.isEmpty()) svg.style(style);

		File file = new File(System.getProperty("user.dir")+"/resources/svg/"+getClass().getSimpleName()+".svg");
		if (file.exists()) {
			Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
			StringBuffer sb = new StringBuffer();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (line.startsWith("<svg") || line.endsWith("svg>")) continue;
				for (Entry<String, Object> replacement : replacements.entrySet()) {
					line = replace(line,replacement);
				}
				sb.append(line+"\n");
			}
			scanner.close();
			svg.content(sb.toString());
			
			if (oneWay != null) {
				switch (oneWay) {
			
				case EAST:
					new Tag("polygon").clazz("oneway").attr("points", "100,50 75,35 75,65").addTo(svg);
					break;
				case WEST:
					new Tag("polygon").clazz("oneway").attr("points", "0,50 25,35 25,65").addTo(svg);
					break;
				default:
				}
			}
		} else {
			new Tag("title").content(t("No display defined for this tile ({})",getClass().getSimpleName())).addTo(svg);
			new Tag("text")
				.pos(35,70)	
				.content("?")
				.addTo(svg);
		}
		
		return svg;
	}
	
	@Override
	public String toString() {
		return t("{}({},{})",getClass().getSimpleName(),x,y) ;
	}
	
	public Train train() {
		return train;
	}
	
	public void train(Train train) throws IOException {
		this.train = train;		
		plan.place(this);
	}	

	public void unlock() throws IOException {
		route = null;
		train = null;
		plan.place(this);
	}

	public Tile update(HashMap<String, String> params) throws IOException {
		LOG.debug("{}.update({})",getClass().getSimpleName(),params);
		String oneWayDir = params.get("oneway");
		if (oneWayDir != null) {
			try {
				oneWay = Direction.valueOf(oneWayDir);
			} catch (Exception e) {
				oneWay = null;
			}
		}
		return this;
	}
	
	/*
	if (clazz == null) throw new NullPointerException(TILE+" must not be null!");
	Class<Tile> tc = Tile.class;		
	clazz = tc.getName().replace(".Tile", "."+clazz);		
	Tile tile = (Tile) tc.getClassLoader().loadClass(clazz).getDeclaredConstructor().newInstance();
	if (tile instanceof Eraser) {
		Tile erased = get(x,y,true);
		remove(erased);
		return erased == null ? null : t("Removed {}.",erased);
	}
	if (configJson != null) tile.configure(new JSONObject(configJson));		
	set(x, y, tile);
	return t("Added {}",tile.getClass().getSimpleName());
}*/
}
