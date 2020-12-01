package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;
import de.srsoftware.web4rail.tags.Select;

public class TextDisplay extends StretchableTile {
	private static final String TEXT = "text";
	private String text = "Hello, world!";
	
	@Override
	public JSONObject json() {
		return super.json().put(TEXT, text);
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(TEXT))	text = json.getString(TEXT);
		return super.load(json);
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

	public static Select selector(TextDisplay preselected,Collection<TextDisplay> exclude) {
		if (isNull(exclude)) exclude = new Vector<TextDisplay>();
		Select select = new Select(TextDisplay.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (Tile tile : plan.tiles.values()) {
			if (!(tile instanceof TextDisplay)) continue;
			if (exclude.contains(tile)) continue;
			Tag opt = select.addOption(tile.id(), tile);
			if (tile == preselected) opt.attr("selected", "selected");
		}
		return select;
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
	public Tile update(HashMap<String, String> params) {
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
