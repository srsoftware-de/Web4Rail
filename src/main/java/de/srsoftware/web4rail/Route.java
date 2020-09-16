package de.srsoftware.web4rail;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Plan.Direction;
import de.srsoftware.web4rail.tiles.Block;
import de.srsoftware.web4rail.tiles.Contact;
import de.srsoftware.web4rail.tiles.Shadow;
import de.srsoftware.web4rail.tiles.Signal;
import de.srsoftware.web4rail.tiles.Tile;
import de.srsoftware.web4rail.tiles.Turnout;
import de.srsoftware.web4rail.tiles.Turnout.State;

public class Route {
	
	private Vector<Tile> path;
	private Vector<Signal> signals;
	private Vector<Contact> contacts;
	private HashMap<Turnout,Turnout.State> turnouts;
	private String id;
	private String name;

	public Tile add(Tile tile, Direction direrction) {
		if (tile instanceof Shadow) tile = ((Shadow)tile).overlay(); 
		path.add(tile);
		if (tile instanceof Contact) contacts.add((Contact) tile);
		if (tile instanceof Signal) {
			Signal signal = (Signal) tile;
			if (signal.isAffectedFrom(direrction)) signals.add(signal);			
		}

		return tile;
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
				if (i==0) {
					sb.append(((Block)tile).name);
				} else if (i==path.size()-1){
					sb.append("-"+((Block)tile).name);
				} else {
					sb.append("-"+tile.x+":"+tile.y);
				}
			}
			id = sb.toString();
		}
		return id;
	}

	public List<Route> multiply(int size) {
		Vector<Route> routes = new Vector<Route>();
		for (int i=0; i<size; i++) routes.add(i==0 ? this : this.clone());
		return routes;
	}
	
	public String name() {
		if (name == null) name = id();
		return name;
	}
	
	public Vector<Tile> path() {
		return new Vector<Tile>(path);
	}
	
	public Window properties() {	
		Window win = new Window("route-properties",t("Properties of {}",this));

		new Tag("h4").content(t("Signals")).addTo(win);
		Tag list = new Tag("ul");
		for (Signal s : signals) Plan.addLink(s,s.toString(),list);
		list.addTo(win);
		
		new Tag("h4").content(t("Contacts")).addTo(win);
		list = new Tag("ul");
		for (Contact c : contacts) Plan.addLink(c,c.toString(),list);
		list.addTo(win);
		
		new Tag("h4").content(t("Turnouts")).addTo(win);
		list = new Tag("ul");
		for (Entry<Turnout, State> entry : turnouts.entrySet()) {
			Turnout turnout = entry.getKey();
			Plan.addLink(turnout, turnout+": "+entry.getValue(), list);
		}
		list.addTo(win);

		return win;
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

	public Block startBlock() {
		return (Block) path.get(0);
	}

	protected static String t(String txt, Object...fills) {
		return Translation.get(Application.class, txt, fills);
	}

	public void setLast(State state) {
		if (state == null || state == State.UNDEF) return;
		Tile lastTile = path.lastElement();
		if (lastTile instanceof Turnout) turnouts.put((Turnout) lastTile,state);
	}
}
