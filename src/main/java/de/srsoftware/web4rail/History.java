package de.srsoftware.web4rail;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import org.json.JSONObject;

import de.srsoftware.web4rail.BaseClass.Id;

public class History {
	
	private static HashMap<BaseClass.Id, Vector<LogEntry>> log = new HashMap<>();
	
	static class LogEntry extends BaseClass {
		private long timestamp;
		private String text;
		
		public LogEntry(String text) {
			this.text = text;
			timestamp = new Date().getTime();
		}

		public Date date() {
			return new Date(timestamp);
		}
		
		public long getTime() {
			return timestamp;
		}
		
		public String getText() {
			return text;
		}
	};
	
	public static LogEntry assign(LogEntry logEntry, BaseClass object) {
		Id id = object.id();
		Vector<LogEntry> list = log.get(id);
		if (list == null) log.put(id, list = new Vector<>());
		list.insertElementAt(logEntry,0);
		return logEntry;
	}
	
	public static Vector<LogEntry> getFor(BaseClass object){
		Vector<LogEntry> list = log.get(object.id());
		return list != null ? list : new Vector<>();
	}
	
	public static Object action(HashMap<String, String> params) {
		
		switch (params.get(Constants.ACTION)) {
			case Constants.ACTION_ADD:
				BaseClass object = BaseClass.get(Id.from(params));
				return object != null ? object.addLogEntry(params.get(Constants.NOTES)) : BaseClass.t("Unknown object!");

		}
		
		return BaseClass.t("Unknown action: {}",params.get(Constants.ACTION));
	}

	public static void save(String filename) {
		try {
			FileWriter file = new FileWriter(filename, Constants.UTF8);
			JSONObject json = new JSONObject();
			log.entrySet().forEach(entry -> {
				JSONObject list = new JSONObject();
				entry.getValue().forEach(le -> list.put(le.timestamp+"", le.getText()));
				json.put(entry.getKey().toString(), list);
			});
			json.write(file);
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void load(String filename) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename, Constants.UTF8));
		JSONObject json = new JSONObject(file.readLine());
		file.close();
		
		for (String id : json.keySet()) {
			JSONObject o = json.getJSONObject(id);
			Vector<LogEntry> entries = new Vector<>();
			
			for (String time : o.keySet()) {
				LogEntry le = new LogEntry(o.getString(time));
				le.timestamp = Long.parseLong(time);
				entries.add(le);
			}
			Collections.sort(entries, (a,b) -> Long.compare(b.timestamp, a.timestamp));
			log.put(new Id(id), entries);
		}

	}
}
