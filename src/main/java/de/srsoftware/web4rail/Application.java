package de.srsoftware.web4rail;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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

	public static void main(String[] args) throws IOException {
		Configuration config = new Configuration(Configuration.dir("Web4Rail")+"/app.config");
		LOG.debug("Config: {}",config);
		plan = new Plan();
		plan.set(0, 0, new StraightH());
		plan.set(1, 0, new DiagSW());
		plan.set(1, 1, new StraightV());
		plan.set(1, 2, new DiagNE());
		plan.set(2, 2, new TurnoutWS());
		plan.set(3, 2, new DiagWN());
		plan.set(3, 1, new TurnoutSE());
		plan.set(3, 0, new TurnoutSW());
		plan.set(2, 0, new EndE());
		plan.set(4, 1, new EndW());
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
		out.write(msg.getBytes(StandardCharsets.UTF_8));
		out.close();
	}

	private static String t(String text, Object...fills) {
		return Translation.get(Application.class, text, fills);
	}

	private static void sendPlan(HttpExchange client) throws IOException {
		send(client,plan.html().style("css/style.css").js("jquery-3.5.1.slim.min.js").js("js/plan.js"));
	}

	private static void send(HttpExchange client, Page response) throws IOException {
		client.getResponseHeaders().set("content-type", "text/plain");
		StringBuffer html = response.html();
		client.getResponseHeaders().add("content-type", "text/html");
        client.sendResponseHeaders(200, html.length());
        OutputStream os = client.getResponseBody();
        os.write(html.toString().getBytes(StandardCharsets.UTF_8));
        os.close();
	}
}
