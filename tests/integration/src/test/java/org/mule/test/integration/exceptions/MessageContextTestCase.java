package org.mule.test.integration.exceptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class MessageContextTestCase extends FunctionalTestCase {

	String request = "Hello World";

	protected String getConfigResources() {
		return "org/mule/test/integration/exceptions/message-context-test.xml";
	}

	/**
	 * Test for MULE-4361
	 * @throws Exception
	 */
	public void testAlternateExceptionStrategy() throws Exception {

		try {
			MuleClient client1 = new MuleClient();
			DefaultMuleMessage msg1 = new DefaultMuleMessage(request, client1.getMuleContext());
			MuleMessage response1 = client1.send("testin", msg1, 200000);
			assertNotNull(response1);
			Thread.sleep(10000); // Wait for test to finish
		} catch (Throwable t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
		}
	}

}
