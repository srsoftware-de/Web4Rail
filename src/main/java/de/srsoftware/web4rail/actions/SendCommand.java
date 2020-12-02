package de.srsoftware.web4rail.actions;

import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;

public class SendCommand extends Action{

	public SendCommand(Context parent) {
		super(parent);
	}

	public static final String COMMAND = "command";
	private String command = "SET 1 POWER OFF";

	@Override
	public boolean fire(Context context) {
		plan.queue(new Command(command) {
			
			@Override
			public void onResponse(Reply reply) {
				super.onResponse(reply);
				plan.stream(reply.message());
			}
		});
	
		return true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(COMMAND, command);
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		command = json.getString(COMMAND);
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Command to send to control unit"),new Input(COMMAND, command));
		return super.properties(preForm, formInputs, postForm);
	}
		
	@Override
	public String toString() {
		return t("Send command \"{}\" to control unit",command);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		command = params.get(COMMAND);
		return properties();
	}
}
