package de.srsoftware.web4rail.tiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.BaseClass;
import de.srsoftware.web4rail.Connector;
import de.srsoftware.web4rail.LoadCallback;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Window;

public abstract class Bridge extends Tile {
	private static final String COUNTERPART = "counterpart";
	private static Bridge pendingConnection = null;
	protected Bridge counterpart = null;
	
	@Override
	public Object click(boolean shift) throws IOException {
		if (pendingConnection != null) return connect();
		return super.click(shift);
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
	public boolean free(Train train) {
		if (isFree()) return true;
		if (!super.free(train)) return false;
		return isSet(counterpart) ? counterpart.free(train) : true;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		if (isSet(counterpart)) json.put(COUNTERPART, counterpart.id().toString());
		return json;
	}
	
	@Override
	public Tile load(JSONObject json) {
		if (json.has(COUNTERPART)) new LoadCallback() {
			@Override
			public void afterLoad() {
				counterpart = (Bridge) plan.get(Id.from(json, COUNTERPART), false);
			}
		};
		return super.load(json);
	}
	
	@Override
	public boolean lockFor(Context context, boolean downgrade) {
		if (lockingTrain() == context.train()) return true;
		if (!super.lockFor(context, downgrade)) return false;
		return isSet(counterpart) ? counterpart.lockFor(context, downgrade) : true;
	}
	
	@Override
	public boolean reserveFor(Context context) {
		if (reservingTrain() == context.train()) return true;
		if (!super.reserveFor(context)) return false;		
		return isSet(counterpart) ? counterpart.reserveFor(context) : true;
	}
	
	@Override
	public boolean setTrain(Train newTrain) {
		if (occupyingTrain() == newTrain) return true;
		if (!super.setTrain(newTrain)) return false;		
		return isNull(counterpart) ? true : counterpart.setTrain(newTrain);		
	}
	
	@Override
	protected Window properties(List<Fieldset> preForm, FormInput formInputs, List<Fieldset> postForm,String...errors) {
		Fieldset fieldset = new Fieldset(t("Counterpart"));
		new Tag("p").content(isSet(counterpart) ? t("Connected to {}.",counterpart) : t("Not connected to other bridge part!")).addTo(fieldset);		
		button(t("Select counterpart"),Map.of(ACTION,ACTION_CONNECT)).addTo(fieldset);
		preForm.add(fieldset);
		return super.properties(preForm, formInputs, postForm,errors);
	}

	public Window propMenu() {
		Window win = new Window("test", "test");
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
	public void removeChild(BaseClass child) {
		if (child == counterpart) counterpart = null;
		super.removeChild(child);
	}
	
	@Override
	public Tag tag(Map<String, Object> replacements) throws IOException {
		Tag tag = super.tag(replacements);
		if (isNull(counterpart)) tag.clazz(tag.get("class")+" disconnected");
		return tag;
	}
}
