package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Fieldset extends Tag {

	private static final long serialVersionUID = -1643025934527173421L;

	public Fieldset(String title) {
		super("fieldset");
		if (title != null) new Tag("legend").content(title).addTo(this);
	}
}
