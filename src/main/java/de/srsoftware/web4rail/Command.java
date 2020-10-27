package de.srsoftware.web4rail;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Command {

	private static final Logger LOG = LoggerFactory.getLogger(Command.class);
	private String command;
	private Reply reply = null;
	
	public static class Reply{
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
			LOG.info("recv {}.{} {} {}.",secs,milis,code,message);
		}		

		public Reply(int code, String message) {
			secs = new Date().getTime();
			milis = (int) (secs % 1000);
			secs /= 1000;
			this.code = code;
			this.message = message;
		}

		public boolean is(int code) {
			return code == this.code;
		}
		
		public boolean succeeded() {
			return (code > 199 && code < 300);
		}
		
		public String message() {
			return message;
		}

		@Override
		public String toString() {
			return "Reply("+secs+"."+milis+" / "+code+" / "+message+")";
		}
	}
		
	public Command(String command) {
		this.command = command;
		LOG.debug("Created new Command({}).",command);
	}
	
	protected void onFailure(Reply reply) {
		LOG.warn("onFailure({})",command);
	}

	public void onResponse(Reply reply) {
		this.reply = reply;
		if (reply.succeeded()) {
			onSuccess();
		} else onFailure(reply);
	}
	
	public void onSuccess(){
		LOG.debug("onSuccess({})",command);
	}
	
	public void readReplyFrom(Scanner scanner) {
		onResponse(new Reply(scanner));
	}

	public Reply reply() throws TimeoutException {
		return reply(100);
	}
	
	public Reply reply(int timeout) throws TimeoutException {
		int counter = 0;
		while (reply == null) try {
			if (counter++ > timeout) timeout();
			Thread.sleep(10);
		} catch (InterruptedException e) {
			LOG.warn("wait() interrupted!",e);
		}
		return reply;
	}

	private void timeout() throws TimeoutException {
		String msg = command;
		command = null;
		throw new TimeoutException("\""+msg+"\" timed out!");		
	}

	@Override
	public String toString() {
		return command;
	}
}
