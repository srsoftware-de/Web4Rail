package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Checkbox extends Tag {

	private static final long serialVersionUID = -7294673319021862994L;

	public Checkbox(String name, String label, boolean preCheck) {
		this(name,label,preCheck,false);
	}
	
	public Checkbox(String name, String label, boolean preCheck, boolean submitDisabled) {
		super("label");
		if (submitDisabled) new Input(name,"off").hideIn(this);
		Tag checkbox = new Tag("input").attr("type", "checkbox").attr("name", name);
		if (preCheck) checkbox.attr("checked", "checked");
		checkbox.addTo(this);
		content(label);		
		
	}
}
