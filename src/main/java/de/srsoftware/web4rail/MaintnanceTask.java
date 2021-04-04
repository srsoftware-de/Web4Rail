package de.srsoftware.web4rail;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONObject;

import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.AddSelect;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tiles.Block;

public class MaintnanceTask extends BaseClass{
	private static final HashSet<String> tasks = new HashSet<>();

	private static final String NAME = "name";

	public static final String INTERVAL = "interval";

	private static final String LAST_DATE = "last_date";

	private static final String LAST_DIST = "last_dist";
	
	private long interval;
	private long lastExecutionDist;
	private Date lastExecutionDate;
	private String name;
		
	public MaintnanceTask(Car car, String name, long interval) {
		this.name = name;
		this.interval = interval;
		parent(car);
		register();
		if (isSet(name)) tasks.add(name);
	}
	
	public static Object action(HashMap<String, String> params) {
		String action = params.get(ACTION);
		if (isNull(action)) return t("No action set!");

		MaintnanceTask task = BaseClass.get(Id.from(params));

		switch (action) {
			case ACTION_ADD:
				Car car = BaseClass.get(Id.from(params, REALM_CAR));
				return car.addTask(createTask(params));
			case ACTION_DROP:
				if (isSet(task)) {
					BaseClass parent = task.parent();
					task.remove();
					return parent.properties();
				}
				return t("No task!");
			case ACTION_START:
				return isSet(task) ? task.executed() : t("No task!");
		}
		String err = t("unknown action: {}",action);
		return (isSet(task)) ? task.parent().properties(err) : err;
	}
	
	private static MaintnanceTask createTask(HashMap<String, String> params) {
		String name = params.get(NAME);
		long interval = Long.parseLong(params.get(INTERVAL));
		Car car = BaseClass.get(Id.from(params, REALM_CAR));
		return new MaintnanceTask(car, name,interval);		
	}
	
	public Button execBtn() {
		return button(t("executed"), Map.of(REALM,REALM_MAINTENANCE,ACTION,ACTION_START));
	}
	
	private Object executed() {
		BaseClass parent = parent();
		if (parent instanceof Car) {
			Car car = (Car) parent;
			lastExecutionDate = new Date();
			lastExecutionDist = car.drivenDistance();
			Train train = car.train();
			if (isSet(train)) {
				Block block = train.currentBlock();
				if (isSet(block)) plan.place(block);
			}
			return car.properties();
		}
		return t("parent is not a car!");		
	}
	
	public long interval() {
		return interval;
	}

	public boolean isDue() {
		return ((Car)parent()).drivenDistance() > lastExecutionDist+interval;
	}
	
	@Override
	public JSONObject json() {
		JSONObject json = super.json();
		json.put(NAME, name);
		json.put(INTERVAL, interval);
		if (isSet(lastExecutionDate)) {
			json.put(LAST_DATE, lastExecutionDate.getTime());
			json.put(LAST_DIST, lastExecutionDist);
		}
		return json;
	}
	
	public Date lastDate() {
		return lastExecutionDate;
	}
	
	public long lastMileage() {
		return lastExecutionDist;
	}
	
	@Override
	public MaintnanceTask load(JSONObject json) {
		json.remove(ID); // we're using the id created by the constructor!
		super.load(json);
		name = json.getString(NAME);
		interval = json.getLong(INTERVAL);
		if (json.has(LAST_DATE)) lastExecutionDate = new Date(json.getLong(LAST_DATE));
		if (json.has(LAST_DIST)) lastExecutionDist = json.getLong(LAST_DIST);
		if (isSet(name)) tasks.add(name);
		return this;
	}
	
	public long nextMileage() {
		return lastExecutionDist+interval;
	}

	public Button removeBtn() {
		return button(t("delete"), Map.of(REALM,REALM_MAINTENANCE,ACTION,ACTION_DROP));
	}

	public static AddSelect selector() {
		AddSelect select = new AddSelect(NAME);
		select.addOption(t("create new task type"));
		tasks.forEach(task -> select.addOption(task));
		return select;
	}

	@Override
	public String toString() {
		return name;
	}
}