package de.srsoftware.web4rail.actions;

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Command;
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Radio;
import de.srsoftware.web4rail.tags.Window;

public class SendCommand extends Action{

	enum Target{
		SYSTEM, SRCP
	}
	
	public SendCommand(BaseClass parent) {
		super(parent);
	}

	public static final String COMMAND = "command";
	private static final String TARGET = "target";
	private String command = "SET 1 POWER OFF";
	private Target target = Target.SRCP;

	@Override
	public boolean fire(Context context) {
		switch (target) {
		case SRCP:
			plan.queue(new Command(command) {
			
				@Override
				public void onResponse(Reply reply) {
					super.onResponse(reply);
					plan.stream(reply.message());
				}
			});
	
			return true;
		case SYSTEM:
			try {
				Runtime.getRuntime().exec(command);
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(COMMAND, command);
		json.put(TARGET, target.toString());
		return json;
	}
	
	@Override
	public Action load(JSONObject json) {
		super.load(json);
		if (json.has(COMMAND)) command = json.getString(COMMAND);
		if (json.has(TARGET)) target = Target.valueOf(json.getString(TARGET));
		return this;	
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Command to send"),new Input(COMMAND, command));
		Tag div = new Tag("div");
		new Radio(TARGET, Target.SYSTEM, t("Operating System"), target == Target.SYSTEM).addTo(div);
		new Radio(TARGET, Target.SRCP, t("SRCP daemon"), target == Target.SRCP).addTo(div);
		formInputs.add(t("Send command to"),div);
		return super.properties(preForm, formInputs, postForm,errors);
	}
	
	@Override
	public String toString() {
		return t("Send command \"{}\" to {}",command,t(target.toString()));
	}
	
	@Override
	protected Object update(Params params) {
		LOG.debug("update: {}",params);
		command = params.getString(COMMAND);
		String t = params.getString(TARGET);
		if (isSet(t)) target = Target.valueOf(t);
		return properties();
	}
}
