package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;

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
	
	private static final HashMap<Id,Relay> relays = new HashMap<Id, Relay>();
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
		relays.put(id(), this);
		return this;
	}
	
	@Override
	public Form propForm(String id) {
		Form form = super.propForm(id);
		Fieldset fieldset = new Fieldset(t("Decoder settings"));
		Label protocol = new Label(t("Protocol:"));
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.protocol).addTo(protocol);
		}
		protocol.addTo(fieldset);
		new Input(ADDRESS, address).numeric().addTo(new Label(t("Address"))).addTo(fieldset).addTo(form);
		fieldset = new Fieldset(t("Ports and Labels"));
		new Input(PORT_A, portA).numeric().addTo(new Label(t("Port for state A"))).addTo(fieldset);
		new Input(LABEL_A, stateLabelA).addTo(new Label(t("Label for state A"))).addTo(fieldset);
		new Input(PORT_B, portB).numeric().addTo(new Label(t("Port for state B"))).addTo(fieldset);
		new Input(LABEL_B, stateLabelB).addTo(new Label(t("Label for state B"))).addTo(fieldset);
		fieldset.addTo(form);
		fieldset = new Fieldset(t("Name"));
		new Input(NAME,name).addTo(new Label(t("Name"))).addTo(fieldset).addTo(form);
		return form;
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

	public static Collection<Relay> list() {
		return relays.values();
	}

	public static Relay get(Id relayId) {
		return relays.get(relayId);
	}
}
