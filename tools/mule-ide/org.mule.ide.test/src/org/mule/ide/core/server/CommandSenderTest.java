package org.mule.ide.core.server;

import junit.framework.TestCase;

/**
 * Test sending commands to a local server.
 * 
 * @author Derek Adams
 */
public class CommandSenderTest extends TestCase {

	/**
	 * Test shutting down a local server.
	 */
	public void testShutdownCommand() {
		MuleCommandSender sender = new MuleCommandSender("localhost", 12345);
		try {
			sender.sendCommand(new MuleShutdownCommand());
		} catch (CommandSendException e) {
			fail(e.getMessage());
		}
	}
}