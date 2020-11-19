package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class TextDisplay extends StretchableTile {
	private static final String TEXT = "text";
	private String text = "Hello, world!";
	
	@Override
	public JSONObject json() {
		return super.json().put(TEXT, text);
	}
	
	@Override
	protected Tile load(JSONObject json) throws IOException {
		super.load(json);
		if (json.has(TEXT))	text = json.getString(TEXT);
		return this;
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",text);
		return super.tag(replacements);
	}
	
	@Override
	public Form propForm(String id) {
		noTrack();
		Form form = super.propForm(id);
		new Tag("h4").content(t("Text")).addTo(form);
		
		new Input(TEXT, text).addTo(new Label(t("Text")+":"+NBSP)).addTo(new Tag("p")).addTo(form);
		
		return form;
	}

	@Override
	protected String stretchType() {
		return t("Width");
	}

	public TextDisplay text(String tx) {
		text = tx;
		return this;
	}
	
	@Override
	public Tile update(HashMap<String, String> params) throws IOException {
		for (Entry<String, String> entry : params.entrySet()) {
			switch (entry.getKey()) {
				case TEXT:
					text(entry.getValue());
					break;
			}
		}
		return super.update(params);
	}
	
	@Override
	public int width() {
		return stretch;
	}
}
