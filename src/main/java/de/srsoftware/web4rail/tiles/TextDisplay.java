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
import de.srsoftware.web4rail.Params;
import de.srsoftware.web4rail.Store;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Select;
import de.srsoftware.web4rail.tags.Window;

public class TextDisplay extends StretchableTile implements Store.Listener {
	private static final String TEXT = "text";
	private String text = "Hello, world!";
	private String displayText = text;
	
	@Override
	public JSONObject json() {
		return super.json().put(TEXT, text);
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(TEXT)) text(json.getString(TEXT));
		return super.load(json);
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		formInputs.add(t("Text"),new Input(TEXT, text));
		return super.properties(preForm, formInputs, postForm,errors);
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
	public void storeUpdated(Store store) {
		displayText = text.replace("{"+store.name()+"}", store.value());
		plan.place(this);
	}

	@Override
	protected String stretchType() {
		return t("Width");
	}
	
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		if (isNull(replacements)) replacements = new HashMap<String, Object>();
		replacements.put("%text%",displayText);
		Tag tag = super.tag(replacements);
		return tag.clazz(tag.get("class")+" fill");
	}

	public TextDisplay text(String tx) {
		text = tx;
		displayText = tx;
		int pos = text.indexOf("{");
		Store.removeListener(this);
		while (pos > -1) {
			int end = text.indexOf("}",pos);
			if (end < 0) break;
			String storeName = text.substring(pos+1, end);
			Store.get(storeName).addListener(this);
			pos = text.indexOf("{",end);
		}
		
		return this;
	}
	
	@Override
	public Tile update(Params params) {
		for (Entry<String, Object> entry : params.entrySet()) {
			switch (entry.getKey()) {
				case TEXT:
					text(entry.getValue().toString());
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
