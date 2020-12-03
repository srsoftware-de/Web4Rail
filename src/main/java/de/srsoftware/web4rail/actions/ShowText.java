package de.srsoftware.web4rail.actions;

import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.tags.Label;

public class ShowText extends TextAction{


	public ShowText(BaseClass parent) {
		super(parent);
	}

	@Override
	public boolean fire(Context context) {
		plan.stream(fill(text,context));
		return true;
	}

	@Override
	protected Label label() {
		return new Label(t("Text to display on clients:")+NBSP);
	}
	
	@Override
	protected void removeChild(BaseClass child) {
		// this class has no child elements		
	}
	
	@Override
	public String toString() {
		return t("Display \"{}\" on clients.",text);
	}
}
