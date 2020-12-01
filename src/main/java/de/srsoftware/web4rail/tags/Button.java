package de.srsoftware.web4rail.tags;

import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;

public class Button extends Tag {

	private static final long serialVersionUID = -7785030725633284515L;

	public Button(String text) {
		super("button");
		attr("type", "submit");
		content(text);
	}
	
	public Button(String text,String action) {
		super("button");
		attr("type","button");
		attr("onclick",action).content(text);
	}
	
	public Button(String text,Form form) {
		this(text,"return submitForm('"+form.get("id")+"');");
	}
	
	public Button(String text, Map<String, ? extends Object> props) {
		this(text,"request("+(new JSONObject(
				props.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().toString()))
				).toString().replace("\"", "'"))+")");
	}
}
