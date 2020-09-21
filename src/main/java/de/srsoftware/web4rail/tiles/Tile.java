package de.srsoftware.web4rail.tiles;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Radio;

public abstract class Tile {
	
	private static final String ID = "id";
	private static final String TYPE = "type";
	private static final String LOCKED = "locked";
	
	private static final String POS = "pos";
	private static final String X = "x";
	private static final Object Y = "y";
	public int x = -1,y = -1;
	
	private static final String ROUTE = "route";
	protected Route route;
	
	private static final String OCCUPIED = "occupied";
	private Train train;
	
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
	
	public JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(ID, id());
		json.put(TYPE, getClass().getSimpleName());
		JSONObject pos = new JSONObject(Map.of(X,x,Y,y));
		json.put(POS, pos);
		if (route != null) json.put(ROUTE, route.id());
		if (oneWay != null) json.put(ONEW_WAY, oneWay);
		return json;
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


	public void configure(JSONObject config) {}
	
	public boolean free() {
		return route == null;
	}
	
	public int height() {
		return 1;
	}
	
	public String id() {
		return "tile-"+x+"-"+y;
	}
	
	public int len() {
		return 1;
	}
	
	public void lock(Route route) throws IOException {
		this.route = route;
		plan.place(this);
	}
	
	public void plan(Plan plan) {
		this.plan = plan;
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
		new Tag("input").attr("type", "hidden").attr("name","action").attr("value", "update").addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","x").attr("value", x).addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","y").attr("value", y).addTo(form);
		
		List<Direction> pd = possibleDirections();
		if (!pd.isEmpty()) {
			new Tag("h4").content(t("One way:")).addTo(form);
			new Radio("oneway","none",t("No"),oneWay == null).addTo(form);		
			for (Direction d:pd) {
				Radio radio = new Radio("oneway",d.toString(),t(d.toString()),d == oneWay);				
				radio.addTo(form);
			}
		}
		return form;
	}
	
	public Tag propMenu() {	
		Window window = new Window("tile-properties",t("Properties of {} @ ({},{})",getClass().getSimpleName(),x,y));
		Tag form = propForm();
		if (form!=null && form.children().size()>3) {
			new Tag("button").attr("type", "submit").content(t("save")).addTo(form);
			form.addTo(window);
		} else {
			window.content(t("This tile ({}) has no properties",getClass().getSimpleName()));
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
	
	public static void saveAll(HashMap<Integer, HashMap<Integer, Tile>> tiles ,String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		for (Entry<Integer, HashMap<Integer, Tile>> column : tiles.entrySet()) {
			for (Entry<Integer, Tile> row : column.getValue().entrySet()) {
				Tile tile = row.getValue();
				if (tile == null || tile instanceof Shadow) continue;
				file.append(tile.json()+"\n");
			}
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
				.id((x!=-1 && y!=-1)?("tile-"+x+"-"+y):(getClass().getSimpleName()))
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

	public Tile update(HashMap<String, String> params) {
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
}
