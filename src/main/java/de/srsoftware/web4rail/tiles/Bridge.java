package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.Map;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.Window;

public abstract class Bridge extends Tile {
	private static Bridge pendingConnection = null;
	protected Bridge counterpart = null;
	
	@Override
	public Object click() throws IOException {
		if (pendingConnection != null) return connect();
		return super.click();
	}
	
	private Object connect() {
		if (this == pendingConnection) return t("Cannot connect {} to itself!",this);
		if (isSet(counterpart)) {
			counterpart.counterpart = null; // drop other connection
			plan.place(counterpart);
		}
		counterpart = pendingConnection;
		counterpart.counterpart = this;
		pendingConnection = null;
		plan.place(this);
		plan.place(counterpart);
		return t("Connected {} and {}.",this,counterpart);
	}
	
	protected abstract Connector connector();

	@Override
	public Window propMenu() {
		Window win = super.propMenu();
		new Tag("h4").content("Counterpart").addTo(win);
		new Tag("p").content(isSet(counterpart) ? t("Connected to {}.",counterpart) : t("Not connected to other bridge part!")).addTo(win);
		button(t("Select counterpart"),Map.of(ACTION,ACTION_CONNECT)).addTo(win);
		return win;
	}

	public Object requestConnect() {
		pendingConnection = this;
		return t("Click other bridge to connect to!");
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		if (isNull(counterpart)) tag.clazz(tag.get("class")+" disconnected");
		return tag;
	}
}
