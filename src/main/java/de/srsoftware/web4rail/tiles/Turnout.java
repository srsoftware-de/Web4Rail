package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.ControlUnit.Reply;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Radio;

public abstract class Turnout extends Tile implements Device{
	public static final String STATE = "state";
	private static final String PORT_A = "port_a";
	private static final String PORT_B = "port_b";	
	protected static final String STRAIGHT = "straight";

	private Protocol protocol = Protocol.DCC128;
	protected int address = 0;
	protected int portA = 0, portB = 1;
	protected int delay = 400;
	protected boolean initialized = false;
	protected boolean error = false;
	
	public enum State{
		LEFT,STRAIGHT,RIGHT,UNDEF;
	}
	
	protected State state = State.STRAIGHT;
	
	@Override
	public Object click() throws IOException {
		LOG.debug(getClass().getSimpleName()+".click()");
		init();
		return super.click();
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
	
	protected void init() {
		if (!initialized) {
			plan.queue("INIT {} GA "+address+" "+proto());
			initialized = true;
		}
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (portA != 0) json.put(PORT_A, portA);
		if (portB != 1) json.put(PORT_B, portB);
		if (address != 0) json.put(ADDRESS, address);
		json.put(PROTOCOL, protocol);
		return json;
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		if (json.has(ADDRESS)) address = json.getInt(ADDRESS);
		if (json.has(PORT_A)) portA = json.getInt(PORT_A);
		if (json.has(PORT_B)) portB = json.getInt(PORT_B);
		if (json.has(PROTOCOL)) protocol = Protocol.valueOf(json.getString(PROTOCOL));
		return super.load(json);
	}
	
	@Override
	public Tag propForm() {
		Tag form = super.propForm();
		Fieldset fieldset = new Fieldset(t("Decoder settings"));
		Label protocol = new Label(t("Protocol:"));
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == this.protocol).addTo(protocol);
		}
		protocol.addTo(fieldset).addTo(form);
		new Input(ADDRESS, address).numeric().addTo(new Label(t("Address"))).addTo(fieldset);
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
	
	public State state() {
		return state;
	}
	
	public abstract CompletableFuture<Reply> state(State newState) throws IOException;
	
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
		Tag tag = super.tag(replacements);
		tag.clazz(tag.get("class")+(" "+state).toLowerCase()+(error?" error":""));
		return tag;
	}
	
	@Override
	public String title() {
		return getClass().getSimpleName()+t("(Address: {}, Ports {} and {})",address,portA,portB);
	}
	
	public void toggle() {
		state = State.STRAIGHT;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		if (params.containsKey(PROTOCOL)) protocol = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) address = Integer.parseInt(params.get(ADDRESS));
		if (params.containsKey(PORT_A)) portA = Integer.parseInt(params.get(PORT_A));
		if (params.containsKey(PORT_B)) portB = Integer.parseInt(params.get(PORT_B));
		return super.update(params);
	}
}
