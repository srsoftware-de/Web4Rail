package de.srsoftware.web4rail;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class ControlUnit extends Thread{
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 4303;
	
	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private boolean stopped = true;
	private LinkedList<String> queue = new LinkedList<String>();
	private Socket socket;

	/**
	 * @return stops the loop at the next interval
	 */
	public ControlUnit end() {
		stopped = true;
		return this;
	}

	
	public static void main(String[] args) {
		new ControlUnit().setEndpoint("127.0.0.1", DEFAULT_PORT).restart();
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
		socket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
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
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		super.start();
	}
}
