package de.srsoftware.web4rail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.keawe.localconfig.Configuration;
import de.keawe.tools.translations.Translation;
import de.srsoftware.tools.Tag;
import de.srsoftware.web4rail.actions.ActionList;
import de.srsoftware.web4rail.conditions.Condition;
import de.srsoftware.web4rail.moving.Car;
import de.srsoftware.web4rail.moving.Locomotive;
import de.srsoftware.web4rail.moving.Train;

/**
 * Entry point class for the Web4Rail application
 * 
 * @author Stephan Richter, SRSoftware
 *
 */
public class Application implements Constants{
	private static Plan plan; // the track layout in use
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	
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
	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Configuration config = new Configuration(Configuration.dir("Web4Rail")+"/app.config");
		LOG.debug("config: {}",config);
		InetSocketAddress addr = new InetSocketAddress(config.getOrAdd(PORT, 8080));
		HttpServer server = HttpServer.create(addr, 0);
		server.createContext("/plan", client -> sendPlan(client));
		server.createContext("/css" , client -> sendFile(client));
		server.createContext("/js" , client -> sendFile(client));
		server.createContext("/stream", client -> stream(client));
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
        	plan = Plan.load("default");
        } catch (FileNotFoundException e) {
        	plan = new Plan();
		}
        Desktop.getDesktop().browse(URI.create("http://"+InetAddress.getLocalHost().getHostName()+":"+config.getInt(PORT)+"/plan"));
	}
	
	/**
	 * helper class creating unique ids for use throuout the application
	 * @return
	 */
	public static int createId() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return new Date().hashCode();
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
	private static Object handle(HashMap<String, String> params) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		
		String realm = params.get(REALM);
		if (realm == null) throw new NullPointerException(REALM+" should not be null!");
		
		String action = params.get(ACTION);
		if (action == null) throw new NullPointerException(ACTION+" should not be null!");

		switch (realm) {
			case REALM_ACTIONS:
				return ActionList.process(params,plan);
			case REALM_CAR:
				return Car.action(params);
			case REALM_CONDITION:
				return Condition.action(params,plan);
			case REALM_CU:
				return plan.controlUnit().process(params);
			case REALM_LOCO:
				return Locomotive.action(params,plan);
			case REALM_PLAN:
				return plan.action(params);
			case REALM_ROUTE:
				return plan.routeAction(params);
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
	private static HashMap<String, String> inflate(String data) {
		//LOG.debug("inflate({})",data);
		HashMap<String, String> params = new HashMap<String, String>();
		if (data == null || data.trim().isEmpty()) return params;
		String[] parts = data.split("&");
		
		for (String part : parts) {
			String[] entry = part.split("=", 2);
			params.put(URLDecoder.decode(entry[0],UTF8),URLDecoder.decode(entry[1], UTF8));
		}
		
		return params;
	}	

	/**
	 * creates a map from url-encoded data
	 * @param data
	 * @return
	 */
	private static HashMap<String, String> inflate(byte[] data) {
		return inflate(new String(data,UTF8));
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
		} else if (response instanceof CompletableFuture) {
			CompletableFuture<?> promise = (CompletableFuture<?>) response;
			promise.thenAccept(object -> {
				try {
					send(client,object);
				} catch (IOException e) {
					LOG.warn("Was not able to send {}!",object);
				}
			}).exceptionally(ex -> {
				try {
					send(client,ex.getMessage());
				} catch (IOException e) {
					LOG.warn("Was not able to send {}!",ex);
				}
				throw new RuntimeException(ex);
			});
			return;
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
			client.getResponseHeaders().add("Content-Type", Files.probeContentType(file.toPath()));
			client.sendResponseHeaders(200, file.length());
			OutputStream out = client.getResponseBody();
			FileInputStream in = new FileInputStream(file);
			in.transferTo(out);
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
			HashMap<String, String> params = inflate(client.getRequestBody().readAllBytes());
			LOG.debug("sendPlan({})",params);

			if (params.isEmpty()) {
				send(client,plan.html());
				return;
			}
			
			Object response = handle(params);
			LOG.debug("response ({}): {}",response.getClass().getSimpleName(),response);
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
	
	/**
	 * shorthand for Translations.get(text,fills)
	 * @param text
	 * @param fills
	 * @return
	 */
	private static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
}
