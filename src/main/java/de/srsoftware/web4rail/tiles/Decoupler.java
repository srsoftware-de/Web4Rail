package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Command.Reply;
import de.srsoftware.web4rail.Device;
import de.srsoftware.web4rail.Protocol;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public abstract class Decoupler extends Tile implements Device{

	protected int      address     = 0;
	private   Protocol protocol    = Protocol.DCC128;
	protected int      port        = 0;
	private   boolean  initialized = false;

	@Override
	public int address() {
		return address;
	}
	
	@Override
	public Object click(boolean shift) throws IOException {
		Object o = super.click(shift);
		if (!shift) engage();
		return o;
	}
	
	public Reply engage() {
		Reply reply = init();
		if (reply != null && !reply.succeeded()) return reply;
		try {
			
			return plan.queue(new Command("SET {} GA "+address+" "+port+" 1 "+100) {
				
				@Override
				public void onSuccess() {
					super.onSuccess();
					plan.place(Decoupler.this);
				}

				@Override
				protected void onFailure(Reply reply) {
					super.onFailure(reply);
					plan.stream(t("Unable to engage \"{}\": {}",Decoupler.this,reply.message()));
				}
				
			}).reply();
		} catch (TimeoutException e) {
			LOG.warn(e.getMessage());			
		}
		return new Reply(417,t("Timeout while trying to switch {}.",this));
	}
	
	protected Reply init() {
		if (address == 0) return new Reply(200,"OK");
		if (!initialized) {
			Command command = new Command("INIT {} GA "+address+" "+proto()) {

				@Override
				public void onSuccess() {
					super.onSuccess();
					initialized = true;
				}

				@Override
				public void onFailure(Reply r) {
					super.onFailure(r);
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
		json.put(PROTOCOL, protocol);
		if (address != 0) json.put(ADDRESS, address);
		json.put(PORT, port);
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(ADDRESS)) address = json.getInt(ADDRESS);
		if (json.has(PROTOCOL)) protocol = Protocol.valueOf(json.getString(PROTOCOL));
		if (json.has(PORT)) port = json.getInt(PORT);
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		Tag div = new Tag("div");
		for (Protocol proto : Protocol.values()) {
			new Radio(PROTOCOL, proto.toString(), t(proto.toString()), proto == protocol).addTo(div);
		}
		formInputs.add(t("Protocol"),div);
		formInputs.add(t("Address"),new Input(ADDRESS, address).numeric());
		formInputs.add(t("Port"),new Input(PORT, port).numeric());

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
	
	public static Select selector(Decoupler preselected, Collection<Decoupler> exclude) {
		if (isNull(exclude)) exclude = new Vector<Decoupler>();
		Select select = new Select(Decoupler.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Decoupler decoupler : BaseClass.listElements(Decoupler.class)) {			
			if (exclude.contains(decoupler)) continue;
			Tag opt = select.addOption(decoupler.id(), decoupler);
			if (decoupler == preselected) opt.attr("selected", "selected");
		}
		return select;
	}
	
	
	@Override
	public String toString() {
		return t("Decoupler")+"("+x+","+y+")";
	}
	
	@Override
	public Tile update(HashMap<String, String> params) {
		if (params.containsKey(PROTOCOL)) protocol = Protocol.valueOf(params.get(PROTOCOL));
		if (params.containsKey(ADDRESS)) {
			int newAddress = Integer.parseInt(params.get(ADDRESS));
			if (newAddress != address) {
				initialized = false;
				address = newAddress;
			}
		}
		String newPort = params.get(PORT);
		if (isSet(newPort)) {
			int npa = Integer.parseInt(newPort);
			if (npa != port) {
				port = npa;
				initialized = false;
			}
		}
		return super.update(params);
	}
}
