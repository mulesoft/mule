package org.mule.ide.core.server;

/**
 * Interface for parties interested in listening to events sent to a MuleServerController.
 * 
 * @author Derek Adams
 */
public interface IMuleCommandListener {

	/**
	 * Called when a command is receved by the server controller.
	 * 
	 * @param command the command received
	 */
	public void commandReceived(MuleCommand command);
}