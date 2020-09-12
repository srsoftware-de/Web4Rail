package de.srsoftware.web4rail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.keawe.localconfig.Configuration;
import de.keawe.tools.translations.Translation;
import de.srsoftware.web4rail.tiles.DiagNE;
import de.srsoftware.web4rail.tiles.DiagSW;
import de.srsoftware.web4rail.tiles.DiagWN;
import de.srsoftware.web4rail.tiles.EndE;
import de.srsoftware.web4rail.tiles.EndW;
import de.srsoftware.web4rail.tiles.StraightH;
import de.srsoftware.web4rail.tiles.StraightV;
import de.srsoftware.web4rail.tiles.TurnoutSE;
import de.srsoftware.web4rail.tiles.TurnoutSW;
import de.srsoftware.web4rail.tiles.TurnoutWS;

public class Application {
	private static Plan plan;
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	private static final String PORT = "port";
	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Configuration config = new Configuration(Configuration.dir("Web4Rail")+"/app.config");
		LOG.debug("Config: {}",config);
		plan = Plan.load("default.plan");
		/*plan = new Plan();
		plan.set(0, 0, new StraightH());
		plan.set(1, 0, new DiagSW());
		plan.set(1, 1, new StraightV());
		plan.set(1, 2, new DiagNE());
		plan.set(2, 2, new TurnoutWS());
		plan.set(3, 2, new DiagWN());
		plan.set(3, 1, new TurnoutSE());
		plan.set(3, 0, new TurnoutSW());
		plan.set(2, 0, new EndE());
		plan.set(4, 1, new EndW());*/
		InetSocketAddress addr = new InetSocketAddress(config.getOrAdd(PORT, 8080));
		HttpServer server = HttpServer.create(addr, 0);
		server.createContext("/plan", client -> sendPlan(client));
		server.createContext("/css" , client -> sendFile(client));
		server.createContext("/js" , client -> sendFile(client));
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        Desktop.getDesktop().browse(URI.create("http://localhost:"+config.getInt(PORT)+"/plan"));
	}

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

	private static void sendError(HttpExchange client, int code, String msg) throws IOException {
		client.sendResponseHeaders(code, msg.length());
		LOG.error(msg);
		OutputStream out = client.getResponseBody();
		out.write(msg.getBytes(UTF8));
		out.close();
	}

	private static HashMap<String, String> inflate(byte[] data) {
		return inflate(new String(data,UTF8));
	}

	private static HashMap<String, String> inflate(String data) {
		LOG.debug("inflate({})",data);
		HashMap<String, String> params = new HashMap<String, String>();
		if (data == null || data.trim().isEmpty()) return params;
		String[] parts = data.split("&");
		
		for (String part : parts) {
			String[] entry = part.split("=", 2);
			params.put(URLDecoder.decode(entry[0],UTF8),URLDecoder.decode(entry[1], UTF8));
		}
		
		return params;
	}

	private static void send(HttpExchange client, Page response) throws IOException {
		byte[] html = response.html().toString().getBytes(UTF8);
		client.getResponseHeaders().add("content-type", "text/html");
        client.sendResponseHeaders(200, html.length);
        OutputStream os = client.getResponseBody();
        os.write(html);
        os.close();
	}
	
	private static void send(HttpExchange client, String response) throws IOException {
		byte[] html = response.getBytes(UTF8);
		client.getResponseHeaders().add("content-type", "text/plain");
        client.sendResponseHeaders(200, html.length);
        OutputStream os = client.getResponseBody();
        os.write(html);
        os.close();
	}
	
	private static void sendPlan(HttpExchange client) throws IOException {
		HashMap<String, String> params = inflate(client.getRequestBody().readAllBytes());
		try {
			if (!params.isEmpty()) {
				send(client,plan.process(params));
			} else send(client,plan.html().style("css/style.css").js("js/jquery-3.5.1.min.js").js("js/plan.js"));
		} catch (Exception e) {
			LOG.error("Error during sendPlan(): {}",e);
			send(client,new Page().append(e.getMessage()));
		}		
	}
	
	private static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
}
