package de.srsoftware.web4rail.actions;

import java.util.HashMap;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class SendCommand extends Action{

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
	
		return false;
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
	public Window properties(HashMap<String, String> params) {
		Window win = super.properties(params);
		Form form = new Form("action-prop-form-"+id);
		new Input(REALM,REALM_ACTIONS).hideIn(form);
		new Input(ID,params.get(ID)).hideIn(form);
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(CONTEXT,params.get(CONTEXT)).hideIn(form);
		Label label = new Label(t("Command to send to control unit:")+NBSP);
		new Input(COMMAND, command).addTo(label).addTo(form);
		new Button(t("Apply"),form).addTo(form).addTo(win);		
		return win;
	}
	
	@Override
	public String toString() {
		return t("Send command \"{}\" to control unit",command);
	}
	
	@Override
	protected Object update(HashMap<String, String> params) {
		LOG.debug("update: {}",params);
		String error = null;
		command = params.get(COMMAND);
		Window win = properties(params);
		return new Tag("span").content(error).addTo(win);
	}
}
