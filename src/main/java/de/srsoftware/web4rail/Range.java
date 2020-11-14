package de.srsoftware.web4rail;

import java.util.Map;

import org.json.JSONObject;

/**
 * Class for integer ranges (min…max)
 * @author Stephan Richter
 */
public class Range extends BaseClass{
	private static final String MAX = "max";
	private static final String MIN = "min";
	
	public int min=0,max=10000;
	
	public JSONObject json() {
		return new JSONObject(Map.of(MIN,min,MAX,max));
	}
	
	public Range load(JSONObject json) {
		min = json.getInt(MIN);
		max = json.getInt(MAX);
		return this;
	}

	public int random() {
		if (max - min == 0) return max - min;
		return min + random.nextInt(max - min);
	}

	@Override
	public String toString() {
		return min+"…"+max;
	}

	public void validate() {
		if (min>max) {
			int dummy = min;
			min = max;
			max = dummy;
		}
	}
}
