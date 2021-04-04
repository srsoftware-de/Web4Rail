package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class AddSelect extends Input {
	private static final long serialVersionUID = -2168654457876014503L;
	private Tag dataList;	

	public AddSelect(String name) {
		super("select");
		attr("name",name);
		attr("list","list-options");
		dataList = new Tag("datalist").id("list-options");
		dataList.addTo(this);
	}
	
	public Tag addOption(Object value) {
		return addOption(value, value);
	}

	public Tag addOption(Object value, Object text) {
		Tag option = new Tag("option").attr("value", value.toString()).content(text.toString());
		option.addTo(dataList);
		return option;
	}
}