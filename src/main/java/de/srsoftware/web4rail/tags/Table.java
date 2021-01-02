package de.srsoftware.web4rail.tags;

import de.srsoftware.tools.Tag;

public class Table extends Tag{

	private static final long serialVersionUID = -5262135863426267225L;
	
	public Table() {
		super("table");
	}

	public Tag addRow(Object...cols) {
		Tag row = new Tag("tr");
		for (Object column : cols) {
			Tag col = null;
			if (column instanceof Tag) {
				Tag child = (Tag) column;
				if (child.is("td") || child.is("th")) col = child;
				if (col == null) col = child.addTo(new Tag("td"));
			} else col = new Tag("td").content(column == null ? "" : column.toString());
			col.addTo(row);
		}
		row.addTo(this);
		return row;
	}
	
	public Table addHead(Object...cols) {
		Object[] tags = new Tag[cols.length];
		for (int i=0; i<cols.length; i++) tags[i]= cols[i] instanceof Tag ? ((Tag)cols[i]).addTo(new Tag("th")) : new Tag("th").content(cols[i].toString());
		addRow(tags);
		return this;
	}
}
