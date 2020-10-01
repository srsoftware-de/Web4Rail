package de.srsoftware.web4rail;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlUnit extends Thread{	
	private static final Logger LOG = LoggerFactory.getLogger(ControlUnit.class);
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 4303;
	private static final int OK_PROTO = 201;
	private static final int OK_MODE = 202;
	
	private class Reply{
		private long secs;
		private int milis;
		private int code;
		private String message;
		
		public Reply(Scanner scanner) {
			String word = scanner.next();
			secs = Long.parseLong(word.substring(0, word.length()-4));
			milis = Integer.parseInt(word.substring(word.length()-3));
			code = scanner.nextInt();
			message = scanner.nextLine().trim();
			LOG.debug("recv {}.{} {} {}.",secs,milis,code,message);
		}

		@Override
		public String toString() {
			return "Reply("+secs+"."+milis+" / "+code+" / "+message+")";
		}
	}
	
	
	
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private boolean stopped = true;
	private LinkedList<String> queue = new LinkedList<String>();
	private Socket socket;
	private Scanner scanner;

	/**
	 * @return stops the loop at the next interval
	 */
	public ControlUnit end() {
		stopped = true;
		return this;
	}

	
	public static void main(String[] args) throws InterruptedException {
		ControlUnit cu = new ControlUnit().setEndpoint("Modellbahn", DEFAULT_PORT).restart();
		Thread.sleep(1000);
		cu.queue("SET 0 GL 1 0 10");
		Thread.sleep(1000);
		cu.end();
	}
	
	public void queue(String command) {
		queue.add(command);
	}

	/**
	 * Should close the server connection and establish new server connection
	 * @return 
	 */
	private ControlUnit restart() {
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
				} else send(queue.poll());
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

	/**
	 * send command to Server
	 * @param command
	 * @throws IOException 
	 */
	private void send(String command) throws IOException {
		if (command == null) return;
		writeln(command);
		Reply reply = new Reply(scanner);
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
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		super.start();
	}


	private void handshake() throws IOException {
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
			writeln("SET PROTOCOL SRCP "+proto);			
		} else throw new IOException("Handshake expected.");
		
		Reply reply = new Reply(scanner);
		if (reply.code != OK_PROTO) throw new IOException("Handshake failed: "+reply);
		
		writeln("SET CONNECTIONMODE SRCP COMMAND");
		reply = new Reply(scanner);
		if (reply.code != OK_MODE) throw new IOException("Handshake failed: "+reply);
	}

	private void writeln(String data) throws IOException {
		socket.getOutputStream().write((data+"\n").getBytes(StandardCharsets.US_ASCII));
		LOG.debug("sent {}.",data);
	}
}
