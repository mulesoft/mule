package org.mule.ide.core.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleServer;

public class MuleServerController implements IMuleCommandListener {

	/** Static logger instance */
	private static transient Log LOGGER = LogFactory.getLog(MuleServerController.class);

	/** Holds the list of config resources for the Mule server */
	private String configResources;

	/** The port on which to listen for commands */
	private int listenPort;

	/** The wrapped Mule server instance */
	private MuleServer muleServer;

	/** Listens for commands from an external source */
	private MuleCommandReceiver commandReceiver;

	public MuleServerController(String configResources, int listenPort) {
		setConfigResources(configResources);
		setListenPort(listenPort);
		createServer();
		startServer();
		startCommandListener();
	}

	/**
	 * Create the server instance.
	 */
	protected void createServer() {
		LOGGER.info("Controller creating MuleServer ...");
		setMuleServer(new MuleServer(getConfigResources()));
	}

	/**
	 * Start the internal server instance.
	 */
	protected void startServer() {
		LOGGER.info("Controller starting MuleServer ...");
		getMuleServer().start(false);
	}

	/**
	 * Start listening for commands in a separate thread.
	 */
	protected void startCommandListener() {
		setCommandReceiver(new MuleCommandReceiver(getListenPort()));
		getCommandReceiver().addMuleCommandListener(this);
		LOGGER.info("Controller starting command listener ...");
		new Thread(getCommandReceiver()).start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.server.IMuleCommandListener#commandReceived(org.mule.ide.core.server.MuleCommand)
	 */
	public void commandReceived(MuleCommand command) {
		LOGGER.info("Command '" + command.getDescription() + "' received by controller.");
		command.executeOn(getMuleServer());
	}

	/**
	 * Use arguments to create controller.
	 * 
	 * @param args the argument array
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("Invalid arguments provided to controller.");
		}
		new MuleServerController(args[0], Integer.parseInt(args[1]));
	}

	protected void setConfigResources(String configResources) {
		this.configResources = configResources;
	}

	protected String getConfigResources() {
		return configResources;
	}

	protected void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	protected int getListenPort() {
		return listenPort;
	}

	protected void setMuleServer(MuleServer server) {
		this.muleServer = server;
	}

	protected MuleServer getMuleServer() {
		return muleServer;
	}

	protected void setCommandReceiver(MuleCommandReceiver commandReceiver) {
		this.commandReceiver = commandReceiver;
	}

	protected MuleCommandReceiver getCommandReceiver() {
		return commandReceiver;
	}
}