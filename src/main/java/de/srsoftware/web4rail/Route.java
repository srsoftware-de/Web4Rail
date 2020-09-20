package de.srsoftware.web4rail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class Route {
	private static final Logger LOG = LoggerFactory.getLogger(Route.class);
	static final String NAME = "name";
	private static final HashMap<String,String> names = new HashMap<String, String>();
	static final String PATH = "path";
	static final String SIGNALS = "signals";
	static final String TURNOUTS = "turnouts";
	private Vector<Tile> path;
	private Vector<Signal> signals;
	private Vector<Contact> contacts;
	private HashMap<Turnout,Turnout.State> turnouts;
	private String id;

	public Tile add(Tile tile, Direction direrction) {
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay(); 
		path.add(tile);
		if (tile instanceof Contact) contacts.add((Contact) tile);
		if (tile instanceof Signal) {
			Signal signal = (Signal) tile;
			if (signal.isAffectedFrom(direrction)) addSignal(signal);			
		}

		return tile;
	}	
	
	void addSignal(Signal signal) {
		signals.add(signal);
	}
	
	void addTurnout(Turnout t, State s) {
		turnouts.put(t, s);
	}

	protected Route clone() {
		Route clone = new Route();
		clone.contacts = new Vector<Contact>(contacts);
		clone.signals = new Vector<Signal>(signals);
		clone.turnouts = new HashMap<>(turnouts);
		clone.path = new Vector<>(path);
		return clone;
	}
	
	public String id() {
		if (id == null) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<path.size();i++) {
				Tile tile = path.get(i);
				if (i>0) sb.append(" – ");
				if (tile instanceof Block) {
					sb.append(((Block)tile).name);
					if (i>0) break; // Kontakt nach dem Ziel-Block nicht mitnehmen
				} else {
					sb.append(tile.x+":"+tile.y);
				}
			}
			id = sb.toString();
		}
		return id;
	}
	
	public boolean inUse() {
		return false;
	}

		
	public String json() {
		JSONObject props = new JSONObject();
		JSONArray path = new JSONArray();
		for (Tile t : this.path) path.put(new JSONObject(Map.of("x",t.x,"y",t.y)));
		props.put(PATH, path);
		
		JSONArray signals = new JSONArray();
		for (Tile t : this.signals) signals.put(new JSONObject(Map.of("x",t.x,"y",t.y)));
		props.put(SIGNALS, signals);
		
		JSONArray turnouts = new JSONArray();
		for (Entry<Turnout, State> entry : this.turnouts.entrySet()) {
			Turnout t = entry.getKey();
			turnouts.put(new JSONObject(Map.of("x",t.x,"y",t.y,Turnout.STATE,entry.getValue())));
		}
		props.put(TURNOUTS, turnouts);
		
		if (names.containsKey(id())) props.put(NAME, names.get(id));

		return props.toString();
	}

	public Route lock(Train train) throws IOException {
		for (Entry<Turnout, State> entry : turnouts.entrySet()) {
			entry.getKey().state(entry.getValue());
		}
		for (Tile tile : path) tile.lock(train);
		return this;
	}

	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}
	
	public String name() {
		String name = names.get(id());
		return name == null ? id() : name;
	}
	
	public void name(String name) {
		if (name.isEmpty()) {
			names.remove(id());
		} else names.put(id(),name);
	}
	
	public Vector<Tile> path() {
		Vector<Tile> result = new Vector<Tile>();
		if (path != null) result.addAll(path);
		return result;
	}
	
	public Window properties() {	
		Window win = new Window("route-properties",t("Properties of {}",this));

		if (!signals.isEmpty()) {
			new Tag("h4").content(t("Signals")).addTo(win);
			Tag list = new Tag("ul");
			for (Signal s : signals) Plan.addLink(s,s.toString(),list);
			list.addTo(win);
		}
		
		if (!contacts.isEmpty()) {
			new Tag("h4").content(t("Contacts")).addTo(win);
			Tag list = new Tag("ul");
			for (Contact c : contacts) Plan.addLink(c,c.toString(),list);
			list.addTo(win);
		}
		
		if (!turnouts.isEmpty()) {
			new Tag("h4").content(t("Turnouts")).addTo(win);
			Tag list = new Tag("ul");
			for (Entry<Turnout, State> entry : turnouts.entrySet()) {
				Turnout turnout = entry.getKey();
				Plan.addLink(turnout, turnout+": "+entry.getValue(), list);
			}
			list.addTo(win);
		}
		
		Tag form = propForm();
		new Tag("button").attr("type", "submit").content(t("save")).addTo(form);
		form.addTo(win);

		return win;
	}
	
	public Tag propForm() {
		Form form = new Form();
		new Tag("input").attr("type", "hidden").attr("name","action").attr("value", "update").addTo(form);
		new Tag("input").attr("type", "hidden").attr("name","route").attr("value", id()).addTo(form);
		
		Tag label = new Tag("label").content(t("name:"));
		new Tag("input").attr("type", "text").attr(NAME,"name").attr("value", name()).addTo(label);		
		label.addTo(form);
		
		return form;
	}

	public Route start(Block block) {
		contacts = new Vector<Contact>();
		signals = new Vector<Signal>();
		path = new Vector<Tile>();
		turnouts = new HashMap<>();
		path.add(block);
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+name()+")";
	}

	public void setLast(State state) {
		if (state == null || state == State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) addTurnout((Turnout) lastTile,state);
	}
	
	public Route setSignals(String state) throws IOException {
		for (Signal signal : signals) signal.state(state == null ? "go" : state);
		return this;
	}

	public Block startBlock() {
		return (Block) path.get(0);
	}		
	
	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}
	
	public Route unlock() {
		for (Tile tile : path) tile.unlock();
		return this;
	}

	public void update(HashMap<String, String> params) {
		LOG.debug("update({})",params);
		if (params.containsKey(NAME)) name(params.get(NAME));
	}
}
