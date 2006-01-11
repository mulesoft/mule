package org.mule.ide.core.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Receives commands on a given port and distributes them to registered listeners.
 * 
 * @author Derek Adams
 */
public class MuleCommandReceiver implements Runnable {

	/** Static logger instance */
	private static transient Log LOGGER = LogFactory.getLog(MuleCommandReceiver.class);

	/** The port to listen on */
	private int port;

	/** The list of listeners interested in commands */
	private List commandListeners = new ArrayList();

	public MuleCommandReceiver(int port) {
		setPort(port);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			ServerSocket commandSocket = new ServerSocket(getPort());
			LOGGER.info("Mule server controller listening for commands on port '" + getPort()
					+ "'.");
			while (true) {
				Socket incoming = commandSocket.accept();
				ObjectInputStream input = new ObjectInputStream(incoming.getInputStream());
				try {
					Object commandObj = input.readObject();
					if (commandObj instanceof MuleCommand) {
						MuleCommand command = (MuleCommand) commandObj;
						fireCommand(command);
					} else {
						LOGGER.error("Command received was not a MuleCommand.");
					}
				} catch (ClassNotFoundException e) {
					LOGGER.error("Class not found for command.", e);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Unable to start IDE command listener socket.", e);
		}
	}

	/**
	 * Fire a command to all registered listeners.
	 * 
	 * @param command the command to fire
	 */
	protected void fireCommand(MuleCommand command) {
		Iterator it = getCommandListeners().iterator();
		while (it.hasNext()) {
			IMuleCommandListener listener = (IMuleCommandListener) it.next();
			listener.commandReceived(command);
		}
	}

	/**
	 * Add a command listener.
	 * 
	 * @param listener the listener to add.
	 */
	public void addMuleCommandListener(IMuleCommandListener listener) {
		getCommandListeners().add(listener);
	}

	/**
	 * Remove a command listener.
	 * 
	 * @param listener the listener to remove.
	 */
	public void removeMuleCommandListener(IMuleCommandListener listener) {
		getCommandListeners().remove(listener);
	}

	/**
	 * @return Returns the commandListeners.
	 */
	protected List getCommandListeners() {
		return commandListeners;
	}

	/**
	 * @param commandListeners The commandListeners to set.
	 */
	protected void setCommandListeners(List commandListeners) {
		this.commandListeners = commandListeners;
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