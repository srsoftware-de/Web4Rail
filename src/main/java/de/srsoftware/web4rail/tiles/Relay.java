package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Select;

public class Relay extends Tile implements Device{
	public static final String STATE = "state";
	private static final String PORT_A = "port_a";
	private static final String PORT_B = "port_b";	
	protected static final String STRAIGHT = "straight";
	public static final String DEFAULT_LABEL_A = "A";
	public static final String DEFAULT_LABEL_B = "B";

	private Protocol protocol = Protocol.DCC128;
	protected int address = 0;
	protected int portA = 0, portB = 1;
	protected int delay = 400;
	protected boolean initialized = false;
	protected boolean error = false;
	public String stateLabelA = DEFAULT_LABEL_A;
	public String stateLabelB = DEFAULT_LABEL_B;
	private String name = t("Relay");
	protected boolean state = true;
	
	public static final boolean STATE_A = true,STATE_B=false;
	private static final String LABEL_A = "label_a";
	private static final String LABEL_B = "label_b";
	private static final String NAME = "name";
	
	public int address() {
		return address;
	}
	
	@Override
	public Object click() throws IOException {
		Object o = super.click();
		state(!state);
		return o;
	}

	protected String commandFor(boolean newState) {
		return "SET {} GA "+address+" "+(newState?portA:portB)+" 1 "+delay;		
	}
	
	public void error(Reply reply) {
		this.error = true;
		try {
			plan.stream(tag(null).toString());
		} catch (IOException e) {
			LOG.error("Was not able to stream: ",e);
		}
		throw new RuntimeException(reply.message()); 
	}
	
	protected Reply init() {
		if (!initialized) {
			Command command = new Command("INIT {} GA "+address+" "+proto()) {

				@Override
				public void onSuccess() {
					super.onSuccess();
					initialized = true;
				}

				@Override
				public void onFailure(Reply r) {
					super.onSuccess();
					initialized = false;					
				}
				
			};			
			try {
				return plan.queue(command).reply();
			} catch (TimeoutException e) {
				LOG.warn(e.getMessage());
			}
		}
		return new Reply(200, "OK");
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (portA != 0) json.put(PORT_A, portA);
		if (portB != 1) json.put(PORT_B, portB);
		if (address != 0) json.put(ADDRESS, address);
		json.put(PROTOCOL, protocol);
		json.put(LABEL_A, stateLabelA);
		json.put(LABEL_B, stateLabelB);
		json.put(NAME, name);
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(ADDRESS)) address = json.getInt(ADDRESS);
		if (json.has(PORT_A)) portA = json.getInt(PORT_A);
		if (json.has(PORT_B)) portB = json.getInt(PORT_B);
		if (json.has(LABEL_A)) stateLabelA = json.getString(LABEL_A);
		if (json.has(LABEL_B)) stateLabelB = json.getString(LABEL_B);
		if (json.has(PROTOCOL)) protocol = Protocol.valueOf(json.getString(PROTOCOL));
		if (json.has(NAME)) name = json.getString(NAME);
		return super.load(json);
	}
	
	@Override
	public Tile position(int x, int y) {
		super.position(x, y);
		return this;
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Name"),new Input(NAME,name));
		Tag div = new Tag("div");
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.protocol).addTo(div);
		}
		formInputs.add(t("Decoder address"),div);
		formInputs.add(t("Address"),new Input(ADDRESS, address).numeric());
		formInputs.add(t("Port for state A"),new Input(PORT_A, portA).numeric());
		formInputs.add(t("Label for state A"),new Input(LABEL_A, stateLabelA));
		formInputs.add(t("Port for state B"),new Input(PORT_B, portB).numeric());
		formInputs.add(t("Label for state B"),new Input(LABEL_B, stateLabelB));
		return super.properties(preForm, formInputs, postForm);
	}
		
	private char proto() {
		switch (protocol) {
		case DCC14:
		case DCC27:
		case DCC28:
		case DCC128:
			return 'N';
		case MOTO:
			return 'M';
		case SELECTRIX:
			return 'S';
		default:
			return 'P';
		}		
	}
		
	public Relay setLabel(boolean state, String tx) {
		if (state) {
			stateLabelA = tx;
		} else stateLabelB = tx;
		return this;
	}
	
	public boolean state() {
		return state;
	}
	
	public Reply state(boolean newState) {
		Reply reply = init();
		if (reply != null && !reply.succeeded()) return reply;
		LOG.debug("Setting {} to {}",this,newState);
		try {
			String cmd = commandFor(newState);
			return plan.queue(new Command(cmd) {
				
				@Override
				public void onSuccess() {
					super.onSuccess();
					Relay.this.state = newState;
					plan.place(Relay.this);
				}

				@Override
				protected void onFailure(Reply reply) {
					super.onFailure(reply);
					plan.stream(t("Unable to switch \"{}\": {}",Relay.this,reply.message()));
				}
				
			}).reply();
		} catch (TimeoutException e) {
			LOG.warn(e.getMessage());			
		}
		return new Reply(417,t("Timeout while trying to switch {}.",this));		
	}
	
	public void success() {
		this.error = false;
		try {
			plan.stream(tag(null).toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (replacements == null) replacements = new HashMap<String, Object>();
		replacements.put("%text%",state ? stateLabelA : stateLabelB);
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+(" "+state).toLowerCase()+(error?" error":""));
		return tag;
	}
	
	@Override
	public String title() {
		return name;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(PROTOCOL)) protocol = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) address = Integer.parseInt(params.get(ADDRESS));
		if (params.containsKey(PORT_A)) portA = Integer.parseInt(params.get(PORT_A));
		if (params.containsKey(PORT_B)) portB = Integer.parseInt(params.get(PORT_B));
		if (params.containsKey(LABEL_A)) stateLabelA = params.get(LABEL_A);
		if (params.containsKey(LABEL_B)) stateLabelB = params.get(LABEL_B);
		if (params.containsKey(NAME)) name = params.get(NAME);
		return super.update(params);
	}

	public static Select selector(Relay preselected, Collection<Relay> exclude) {
		if (isNull(exclude)) exclude = new Vector<Relay>();
		Select select = new Select(Relay.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Relay relay : BaseClass.listElements(Relay.class)) {			
			if (exclude.contains(relay)) continue;
			Tag opt = select.addOption(relay.id(), relay);
			if (relay == preselected) opt.attr("selected", "selected");
		}
		return select;
	}
}
