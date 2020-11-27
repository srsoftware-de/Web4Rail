package de.srsoftware.web4rail;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;

import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.tags.Button;

public abstract class BaseClass implements Constants{
	protected static Plan plan; // the track layout in use
	public static final Random random = new Random();
	public static String speedUnit = DEFAULT_SPEED_UNIT;
	private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();
	public static Button contextButton(String context,String text) {
		String[] parts = context.split(":");
		String realm = parts[0];
		String id = parts.length>1 ? parts[1] : null;
		return new Button(text,Map.of(REALM,realm,ID,id,ACTION,ACTION_PROPS));
	}
	
	public static Tag link(String tagClass,Map<String,Object> params,Object caption) {
		String json = new JSONObject(params).toString().replace("\"", "'");
		return new Tag(tagClass).clazz("link").attr("onclick","request("+json+")").content(caption.toString());
	}
		
	public static boolean isNull(Object o) {
		return o==null;
	}

	public static boolean isSet(Object o) {
		return o != null;
	}
	
	public static String md5sum(Object o) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(o.toString().getBytes(UTF8));
			StringBuffer sb = new StringBuffer();
			for (byte b : digest.digest()) {
			    sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
			    sb.append(HEX_CHARS[b & 0x0F]);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	public static HashMap<String, String> merged(Map<String, String> base, Map<String, String> overlay) {
		HashMap<String,String> merged = new HashMap<>(base);
		overlay.entrySet().stream().forEach(entry -> merged.put(entry.getKey(), entry.getValue()));
		return merged;
	}
}
