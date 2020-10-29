package de.srsoftware.web4rail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.keawe.tools.translations.Translation;
import de.srsoftware.web4rail.tags.Button;
import de.srsoftware.web4rail.tags.Fieldset;
import de.srsoftware.web4rail.tags.Form;
import de.srsoftware.web4rail.tags.Input;
import de.srsoftware.web4rail.tags.Label;

public class ControlUnit extends Thread implements Constants{	
	private static final Logger LOG = LoggerFactory.getLogger(ControlUnit.class);
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 4303;
	private static final String HOST = "host";
	private static final String PORT = "port";
	private static final String BUS = "bus";
	
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private int bus = 0;
	private boolean stopped = true;
	private LinkedList<Command> queue = new LinkedList<Command>();
	private Socket socket;
	private Scanner scanner;
	private boolean power = false;
	private Plan plan;

	public ControlUnit(Plan plan) {
		this.plan = plan;
	}

	/**
	 * @return stops the loop at the next interval
	 */
	public ControlUnit end() {
		stopped = true;
		return this;
	}

	private void handshake() throws TimeoutException, IOException {
		String proto = null;
		if (scanner.hasNext()) {
			String line = scanner.nextLine();
			LOG.debug("recv: "+line);
			for (String part : line.split(";")) {
				part = part.trim();
				if (part.startsWith("SRCP ")) proto = part.substring(5); 
			}
			if (proto == null) throw new IOException("Handshake failed: "+line);
			if (!proto.startsWith("0.8.")) throw new IOException("Unsupported protocol: "+proto);
		} else throw new IOException("Handshake expected.");

		Command command = new Command("SET PROTOCOL SRCP "+proto); 
		send(command);
		if (!command.reply().succeeded()) throw new IOException("Handshake failed: "+command.reply());
		
		command = new Command("SET CONNECTIONMODE SRCP COMMAND"); // preset following mode: COMMAND MODE
		send(command);
		if (!command.reply().succeeded()) throw new IOException("Handshake failed: "+command.reply());
		
		command = new Command("GO"); // switch mode
		send(command);
		if (!command.reply().succeeded()) throw new IOException("Handshake failed: "+command.reply());
	}
	
	private JSONObject json() {
		JSONObject json = new JSONObject();
		json.put(HOST, host);
		json.put(PORT, port);
		json.put(BUS, bus);
		return json;
	}
	
	public void load(String filename) throws IOException {
		BufferedReader file = new BufferedReader(new FileReader(filename));
		JSONObject json = new JSONObject(file.readLine());
		file.close();
		if (json.has(PORT)) port = json.getInt(PORT);
		if (json.has(BUS)) bus = json.getInt(BUS);
		if (json.has(HOST)) host = json.getString(HOST);
	}
	
	public static void main(String[] args) throws InterruptedException {
		ControlUnit cu = new ControlUnit(null).setEndpoint("Modellbahn", DEFAULT_PORT).setBus(1).restart();
		Thread.sleep(1000);
		cu.queue(new Command("SET {} POWER ON") {

			@Override
			public void onSuccess() {
				LOG.debug("Power on");
			}

			@Override
			public void onFailure(Reply reply) {
				LOG.debug("Was not able to turn power on: {}",reply.message());
			}
			
		});
		Thread.sleep(1000);
		cu.end();
	}
	
	public Object process(HashMap<String, String> params) {
		switch (params.get(ACTION)) {
		case ACTION_CONNECT:
			restart();
			return t("Control unit (re)started.");
		case ACTION_EMERGENCY:
			return emergency();
		case ACTION_POWER:
			return togglePower();
		case ACTION_PROPS:
			return properties();
		case ACTION_UPDATE:
			return update(params);
		}
		
		return t("Unknown action: {}",params.get(ACTION));
	}
	
	public Object emergency() {
		power = true;
		return togglePower();
	}

	public Object properties() {
		Window win = new Window("cu-props", t("Properties of the control unit"));
		Form form = new Form();
		new Input(ACTION,ACTION_UPDATE).hideIn(form);
		new Input(REALM,REALM_CU).hideIn(form);
		Fieldset fieldset = new Fieldset(t("Server connection"));
		new Input(HOST,host).addTo(new Label(t("Hostname"))).addTo(fieldset);
		new Input(PORT,port).numeric().addTo(new Label(t("Port"))).addTo(fieldset);
		new Input(BUS,bus).numeric().addTo(new Label(t("Bus"))).addTo(fieldset);
		new Button(t("Save")).addTo(fieldset).addTo(form).addTo(win);
		
		fieldset = new Fieldset("Actions");
		new Button(t("Connect"),"connectCu();").addTo(fieldset).addTo(win);
		return win;
	}

	public Command queue(Command command) {
		queue.add(command);
		return command;
	}
	
	/**
	 * Should close the server connection and establish new server connection
	 * @return 
	 */
	public ControlUnit restart() {
		end();
		start();
		return this;
	}
	
	@Override
	public void run() {		
		while (!stopped) {
			try {	
				if (queue.isEmpty()) {
					Thread.sleep(10);
				} else {
					Command command = queue.pop();
					send(command);
				}
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void save(String filename) throws IOException {
		BufferedWriter file = new BufferedWriter(new FileWriter(filename));
		file.write(json()+"\n");
		file.close();
	}
	
	/**
	 * send command to Server
	 * @param command
	 * @return 
	 * @throws IOException 
	 */
	private void send(Command command) throws IOException {
		if (command == null || command.toString() == null) return;
		String data = command.toString().replace("{}", ""+bus);
		socket.getOutputStream().write((data+"\n").getBytes(StandardCharsets.US_ASCII));
		LOG.info("sent {}.",data);
		command.readReplyFrom(scanner);
	}
	
	private ControlUnit setBus(int bus) {
		this.bus = bus;
		return this;
	}
	
	public ControlUnit setEndpoint(String newHost, int newPort){
		host = newHost;
		port = newPort;
		return this;
	}
	
	@Override
	public synchronized void start() {
		try {
			socket = new Socket(host, port);
			scanner = new Scanner(socket.getInputStream());
			handshake();			
			stopped = false;
		} catch (IOException | TimeoutException e) {
			throw new IllegalStateException(e);
		}
		super.start();
	}
	
	private static String t(String text,Object...fills) {
		return Translation.get(Application.class, text, fills);
	}
	
	private Command togglePower() {
		power = !power;
		String PW = power?"ON":"OFF";
		Command command = new Command("SET {} POWER "+PW) {
			
			@Override
			public void onSuccess() {
				plan.stream(t("Turned power {}.",PW));				
			}

			@Override
			public void onFailure(Reply reply) {
				plan.stream(t("Was not able to turn power {}!",PW));
			}
				
		};
		return queue(command);
	}

	
	public String update(HashMap<String, String> params) {
		if (params.containsKey(HOST)) host = params.get(HOST);
		if (params.containsKey(PORT)) port = Integer.parseInt(params.get(PORT));
		if (params.containsKey(BUS)) bus = Integer.parseInt(params.get(BUS));
		return t("Updated control unit settings");
	}
}
