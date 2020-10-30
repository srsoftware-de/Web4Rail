package de.srsoftware.web4rail;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handles SRCP commands and their replies received from the SRCP daemon
 * @author Stephan Richter, SRSoftware
 *
 */
public class Command {

	private static final Logger LOG = LoggerFactory.getLogger(Command.class);
	private String command;
	private Reply reply = null;
	
	/**
	 * encapsulates a reply received from the SRCP daemon
	 *
	 */
	public static class Reply{
		private long secs;
		private int milis;
		private int code;
		private String message;
		
		/**
		 * parses a reply from the SRCP daemon
		 * @param scanner
		 */
		public Reply(Scanner scanner) {
			String word = scanner.next();
			secs = Long.parseLong(word.substring(0, word.length()-4));
			milis = Integer.parseInt(word.substring(word.length()-3));
			code = scanner.nextInt();
			message = scanner.nextLine().trim();
			LOG.info("recv {}.{} {} {}.",secs,milis,code,message);
		}		

		/**
		 * creates a reply with given data
		 * @param code
		 * @param message
		 */
		public Reply(int code, String message) {
			secs = new Date().getTime();
			milis = (int) (secs % 1000);
			secs /= 1000;
			this.code = code;
			this.message = message;
		}

		/**
		 * checks if a response has a specific code
		 * @param code
		 * @return true if the given code equals the response's code
		 */
		public boolean is(int code) {
			return code == this.code;
		}
		
		/**
		 * @return true, if the response code is between 200 and 300
		 */
		public boolean succeeded() {
			return (code > 199 && code < 300);
		}
		
		/**
		 * @return the message passed along with the response from the SRCP deameon
		 */
		public String message() {
			return message;
		}

		@Override
		public String toString() {
			return "Reply("+secs+"."+milis+" / "+code+" / "+message+")";
		}
	}
		
	/**
	 * encapsulates a command to be send to the SRCP daemon
	 * @param command
	 */
	public Command(String command) {
		this.command = command;
		LOG.debug("Created new Command({}).",command);
	}
	
	/**
	 * called, if the response indicates an error
	 * @param reply
	 */
	protected void onFailure(Reply reply) {
		LOG.warn("onFailure({})",command);
	}

	/**
	 * called, when a response from the SRCP daemon is received
	 * @param reply
	 */
	public void onResponse(Reply reply) {
		this.reply = reply;
		if (reply.succeeded()) {
			onSuccess();
		} else onFailure(reply);
	}
	
	/**
	 * called, when the response from the SRCP daemon indicates success of the operation
	 */
	public void onSuccess(){
		LOG.debug("onSuccess({})",command);
	}
	
	/**
	 * parses the reply from the SRCP daemon
	 * @param scanner
	 */
	public void readReplyFrom(Scanner scanner) {
		onResponse(new Reply(scanner));
	}

	/**
	 * waits for the reply from the SRCP daemon for one second
	 * @return
	 * @throws TimeoutException
	 */
	public Reply reply() throws TimeoutException {
		return reply(100);
	}
	
	/**
	 * waits for the reply from the SRCP daemon for a given timeout
	 * @param timeout time (in 10ms units) to wait, before a timeout is thrown
	 * @return
	 * @throws TimeoutException
	 */
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

	/**
	 * generate a timeout exception
	 * @throws TimeoutException
	 */
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
