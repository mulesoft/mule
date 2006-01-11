package org.mule.ide.core.server;

import java.io.Serializable;

import org.mule.MuleManager;
import org.mule.MuleServer;

public class MuleShutdownCommand extends MuleCommand implements Serializable {

	/** Command id */
	private static final String ID = "shutdown";

	/** Command description */
	private static final String DESCRIPTION = "Shutdown Server Instance";

	public MuleShutdownCommand() {
		super(ID, DESCRIPTION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.ide.core.server.MuleCommand#executeOn(org.mule.MuleServer)
	 */
	public void executeOn(MuleServer server) {
		MuleManager.getInstance().dispose();
	}
}