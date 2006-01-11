package org.mule.ide.core.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Sends commands to a local mule server.
 * 
 * @author Derek Adams
 */
public class MuleCommandSender {

	/** The host that will receive commands */
	private String host;

	/** The port that will receive commands */
	private int port;

	public MuleCommandSender(String host, int port) {
		setPort(port);
	}

	/**
	 * Send a command to the controller.
	 * 
	 * @param command the command
	 * @throws CommandSendException
	 */
	public void sendCommand(MuleCommand command) throws CommandSendException {
		ObjectOutputStream stream = null;
		try {
			Socket socket = new Socket(getHost(), getPort());
			stream = new ObjectOutputStream(socket.getOutputStream());
			stream.writeObject(command);
		} catch (UnknownHostException e) {
			throw new CommandSendException(e.getMessage());
		} catch (IOException e) {
			throw new CommandSendException(e.getMessage());
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					throw new CommandSendException(e.getMessage());
				}
			}
		}
	}

	/**
	 * @return Returns the host.
	 */
	protected String getHost() {
		return host;
	}

	/**
	 * @param host The host to set.
	 */
	protected void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return Returns the port.
	 */
	protected int getPort() {
		return port;
	}

	/**
	 * @param port The port to set.
	 */
	protected void setPort(int port) {
		this.port = port;
	}
}