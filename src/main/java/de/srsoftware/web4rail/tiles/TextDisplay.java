package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Window;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
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
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm) {
		formInputs.add(t("Text"),new Input(TEXT, text));
		return super.properties(preForm, formInputs, postForm);
	}

	public static Select selector(TextDisplay preselected,Collection<TextDisplay> exclude) {
		if (isNull(exclude)) exclude = new Vector<TextDisplay>();
		Select select = new Select(TextDisplay.class.getSimpleName());
		new Tag("option").attr("value","0").content(t("unset")).addTo(select);
		for (TextDisplay display : BaseClass.listElements(TextDisplay.class)) {
			if (exclude.contains(display)) continue;
			Tag opt = select.addOption(display.id(), display);
			if (display == preselected) opt.attr("selected", "selected");
		}
		return select;
	}

	@Override
	protected String stretchType() {
		return t("Width");
	}
	
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",text);
		Tag tag = super.tag(replacements);
		return tag.clazz(tag.get("class")+" fill");
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
		return stretch();
	}
}
