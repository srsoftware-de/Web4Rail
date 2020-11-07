package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Radio extends Tag {

	private static final long serialVersionUID = -7291730168237304236L;

	public Radio(String groupName, Object value, String label, boolean preCheck) {
		super("label");
		Tag radio = new Tag("input").attr("type", "radio").attr("name", groupName).attr("value", ""+value);
		if (preCheck) radio.attr("checked", "checked");
		radio.addTo(this);
		content(label);
		
	}

}
