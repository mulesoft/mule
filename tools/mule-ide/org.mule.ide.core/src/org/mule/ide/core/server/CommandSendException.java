package org.mule.ide.core.server;

/**
 * Exception thrown when a command can not be sent to a server/port.
 * 
 * @author Derek Adams
 */
public class CommandSendException extends Exception {

	public CommandSendException(String message) {
		super(message);
	}
}