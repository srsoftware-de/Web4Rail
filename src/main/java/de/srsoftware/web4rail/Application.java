package de.srsoftware.web4rail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.srsoftware.localconfig.Configuration;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.devices.Decoder;
import de.srsoftware.web4rail.functions.Function;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;
import de.srsoftware.web4rail.tags.Window;
import de.srsoftware.web4rail.tiles.Contact;

/**
 * Entry point class for the Web4Rail application
 * 
 * @author Stephan Richter, SRSoftware
 *
 */
public class Application extends BaseClass{
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	private static final String START_TRAINS = "--start-trains";
	private static final String FILENAME = "filename";
	private static Configuration config;
	private static int threadCounter = 0;
	
	/**
	 * entry point for the application:<br/>
	 * creates a http server, loads a plan and directs a browser to the respective page
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static void main(String[] args) throws IOException {
		config = new Configuration(Configuration.dir("Web4Rail")+"/app.config");
		LOG.debug("config: {}",config);
		InetSocketAddress addr = new InetSocketAddress(config.getOrAdd(PORT, 8080));
		String planName = config.getOrAdd(Plan.NAME, Plan.DEFAULT_NAME);
		HttpServer server = HttpServer.create(addr, 0);
		server.createContext("/", client -> sendPlan(client));
		server.createContext("/plan", client -> sendPlan(client)); // backward compatibility
		server.createContext("/css" , client -> sendFile(client));
		server.createContext("/js" , client -> sendFile(client));
		server.createContext("/stream", client -> stream(client));
        server.start();
        try {
        	Plan.load(planName);
        } catch (IOException e) {
        	plan = new Plan();
		}
        plan.setAppConfig(config);
        try {
			Desktop.getDesktop().browse(URI.create("http://"+InetAddress.getLocalHost().getHostName()+":"+config.getInt(PORT)));
		} catch (IOException e) {
			e.printStackTrace();
		}

        LOG.debug("Processing arguments:");
        for (String arg : args) {
        	LOG.debug("processing {}",arg);
        	switch (arg) {
        		case START_TRAINS:
        			Train.startAll();
        			break;
        	}
        }
	}

	/**
	 * handles request from clients by delegating them to respective classes
	 * @param params
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private static Object handle(Params params) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		LOG.debug("Application.handle({})",params);
		String realm = params.getString(REALM);
		if (isNull(realm)) throw new NullPointerException(REALM+" should not be null!");
		
		String action = params.getString(ACTION);
		if (isNull(action)) throw new NullPointerException(ACTION+" should not be null!");
		
		if (action.equals(ACTION_OPEN)) return open(params);

		switch (realm) {
			case REALM_ACTIONS:
				return ActionList.process(params,plan);
			case REALM_CAR:
				return Car.action(params,plan);
			case REALM_CONTACT:
				return Contact.process(params);
			case REALM_CONDITION:
				return Condition.action(params,plan);
			case REALM_CU:				
				return plan.controlUnit().process(params);
			case REALM_DECODER:
				return Decoder.action(params);
			case REALM_FUNCTION:
				return Function.action(params);
			case REALM_HISTORY:
				return History.action(params);
			case REALM_LOCO:
				return Locomotive.action(params,plan);
			case REALM_LOOKUP:
				return LookupTable.action(params);
			case REALM_MAINTENANCE:
				return MaintnanceTask.action(params);
			case REALM_PLAN:
				return plan.action(params);
			case REALM_ROUTE:
				return Route.action(params);
			case REALM_TRAIN:				
				return Train.action(params,plan);
		}

		return t("Unknown realm: {}",params.get(REALM));
	}
	
	/**
	 * creates a map from url-encoded data 
	 * @param data
	 * @return
	 */
	private static Params inflate(String data) {
		//LOG.debug("inflate({})",data);
		Params params = new Params();
		if (isNull(data) || data.trim().isEmpty()) return params;
		String[] parts = data.split("&");
		
		for (String part : parts) {
			String[] map = part.split("=", 2);
			String key = URLDecoder.decode(map[0],UTF8);
			String value = URLDecoder.decode(map[1], UTF8);
			
			Params level = params;
			while (key.contains("/")) { // root/path/entry=value mappen zu params[root][path][entry]=value
				String[] path = key.split("/", 2);
				key = path[0];
				Object entry = level.get(key);
				if (entry instanceof Params) {
					level = (Params) entry;
				} else {
					Params dummy = new Params();
					level.put(key, dummy);
					level = dummy;					
				}
				key = path[1];
			}
			level.put(key,value);
		}
		
		return params;
	}	

	/**
	 * creates a map from url-encoded data
	 * @param data
	 * @return
	 */
	private static Params inflate(byte[] data) {
		return inflate(new String(data,UTF8));
	}
	
	private static String mimeOf(File file) throws IOException {
		String[] parts = file.toString().split("\\.");
		switch (parts[parts.length-1].toLowerCase()) {
		case "js":
			return "application/javascript";
		case "css":
			return "text/css";
		}
		LOG.warn("No conten type stored for {}!",file);
		return Files.probeContentType(file.toPath());
	}
	
	private static Object open(Params params) {
		Window win = new Window("open-plan", t("Open plan..."));
		String filename = params.getString(FILENAME);
		if (isNull(filename)) {
			filename = ".";
		} else if (filename.startsWith("."+File.separator)) {
			filename = filename.substring(2);
		}
		File file = new File(filename);
		if (file.isDirectory()) {
			Tag ul = null;
			File[] children = file.listFiles();
			for (File child : children) {
				if (child.isDirectory()) {
					if (isNull(ul)) ul = new Tag("ul").addTo(win);
					Plan.link("li", child.getName(), Map.of(REALM,REALM_APP,ACTION,ACTION_OPEN,FILENAME,filename+File.separator+child.getName())).clazz("directory").addTo(ul);
				}
			}
			for (File child : children) {
				if (!child.isDirectory() && child.getName().endsWith(".plan")) {
					if (isNull(ul)) ul = new Tag("ul").addTo(win);
					Plan.link("li", child.getName(), Map.of(REALM,REALM_APP,ACTION,ACTION_OPEN,FILENAME,filename+File.separator+child.getName())).clazz("plan-file").addTo(ul);
				}
			}
		} else {
			if (file.getName().endsWith(".plan")) {
				String name = file.getPath();
				config.put(NAME,name.substring(0,name.length()-5));
				try {
					config.save();
					plan.controlUnit().set(false);
					plan.stream(t("Application will load \"{}\" on next launch and will now quit!",file));
					plan.controlUnit().end();
					System.exit(0);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return win;
	}

	
	/**
	 * sends a response generated from the application to a given client
	 * @param client
	 * @param response
	 * @throws IOException
	 */
	private static void send(HttpExchange client, Object response) throws IOException {
		byte[] html;
		if (response instanceof Page) {
			html = ((Page)response).html().toString().getBytes(UTF8);
			client.getResponseHeaders().add("content-type", "text/html");
		} else {
			html = (response == null ? "" : response.toString()).getBytes(UTF8);	
			client.getResponseHeaders().add("content-type", "text/plain");	
		}
		
        client.sendResponseHeaders(200, html.length);
        OutputStream os = client.getResponseBody();
        os.write(html);
        os.close();
	}

	/**
	 * sends an error to a given client
	 * @param client
	 * @param code
	 * @param msg
	 * @throws IOException
	 */
	private static void sendError(HttpExchange client, int code, String msg) throws IOException {
		client.sendResponseHeaders(code, msg.length());
		LOG.error(msg);
		OutputStream out = client.getResponseBody();
		out.write(msg.getBytes(UTF8));
		out.close();
	}

	/**
	 * sends a requested file to the given client
	 * @param client
	 * @throws IOException
	 */
	private static void sendFile(HttpExchange client) throws IOException {
		URI uri = client.getRequestURI();
		File file = new File(System.getProperty("user.dir")+"/resources"+uri);
		LOG.debug("requesting file: {}",file);
		if (file.exists()) {
			client.getResponseHeaders().add("Content-Type", mimeOf(file));
			client.sendResponseHeaders(200, file.length());
			OutputStream out = client.getResponseBody();
			FileInputStream in = new FileInputStream(file);
			in.transferTo(out);
			out.flush();
			in.close();
			out.close();
			return;
		}
		sendError(client,404,t("Could not find \"{}\"",uri));
	}
	
	/**
	 * sends a response to a given client
	 * @param client
	 * @throws IOException
	 */
	private static void sendPlan(HttpExchange client) throws IOException {
		try {
			Params params = inflate(client.getRequestBody().readAllBytes());
			LOG.debug("sendPlan({})",params);

			if (params.isEmpty()) {
				send(client,plan.html());
				return;
			}
			
			Object response = handle(params);
			if (isSet(response)) {
				if (response instanceof Tag) {
					LOG.debug("response ({}): {}",response.getClass().getSimpleName(),response.toString().substring(0,30)+"...");
				} else LOG.debug("response ({}): {}",response.getClass().getSimpleName(),response);
			}
			send(client,response instanceof String || response instanceof Tag ? response : plan.html());
			
		} catch (Exception e) {
			LOG.error("Error during sendPlan(): {}",e);
			send(client,new Page().append(e.getMessage()));
		}		
	}
	
	/**
	 * establishes an event stream connection between the application and a given client
	 * @param client
	 * @throws IOException
	 */
	private static void stream(HttpExchange client) throws IOException {
		client.getResponseHeaders().set("content-type", "text/event-stream");
		client.sendResponseHeaders(200, 0);
		OutputStreamWriter sseWriter = new OutputStreamWriter(client.getResponseBody());
		plan.addClient(sseWriter);
	}

	public static String threadName(Object owner) {
		return (++threadCounter )+":"+((owner instanceof String) ? (String) owner : owner.getClass().getSimpleName());
	}
}
