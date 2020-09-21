package de.srsoftware.web4rail.tiles;

import java.io.IOException;

import de.srsoftware.tools.Tag;

public abstract class Contact extends Tile{
	
	private boolean active = false;
	private String trigger = null;

	public void activate() throws IOException {
		active = true;
		stream();
		new Thread() {
			public void run() {
				try {
					sleep(200);
					active=false;
					stream();
				} catch (Exception e) {}
			}
		}.start();
		if (route == null) {
			plan.warn(this);
		} else {
			route.contact(this);
		}
	}


	@Override
	public Object click() throws IOException {
		activate();
		return super.click();
	}
	
	public void stream() throws IOException {
		Tag tag = super.tag(null);
		if (active) tag.clazz(tag.get("class")+" active");
		plan.stream("place "+tag);
	}


	public String trigger() {
		if (trigger == null) trigger = getClass().getSimpleName()+"-"+id();
		return trigger;
	}

}
