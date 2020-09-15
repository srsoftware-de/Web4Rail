package de.srsoftware.web4rail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class Application {
	private static Plan plan;
	private static final Logger LOG = LoggerFactory.getLogger(Application.class);
	private static final String PORT = "port";
	private static final Charset UTF8 = StandardCharsets.UTF_8;

	public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Configuration config = new Configuration(Configuration.dir("Web4Rail")+"/app.config");
		LOG.debug("Config: {}",config);
		InetSocketAddress addr = new InetSocketAddress(config.getOrAdd(PORT, 8080));
		HttpServer server = HttpServer.create(addr, 0);
		server.createContext("/plan", client -> sendPlan(client));
		server.createContext("/css" , client -> sendFile(client));
		server.createContext("/js" , client -> sendFile(client));
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        try {
        	plan = Plan.load("default.plan");
        } catch (FileNotFoundException e) {
        	plan = new Plan();
		}
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
	
	private static void sendPlan(HttpExchange client) throws IOException {
		HashMap<String, String> params = inflate(client.getRequestBody().readAllBytes());
		try {
			if (!params.isEmpty()) {
				send(client,plan.process(params));
			} else send(client,plan.html());
		} catch (Exception e) {
			LOG.error("Error during sendPlan(): {}",e);
			send(client,new Page().append(e.getMessage()));
		}		
	}
	
	private static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
}
