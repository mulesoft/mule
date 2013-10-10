/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.jms.scripting;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Defines a scenario when we request a jms message from inside a groovy script
 * which is executed as part of a service whose endpoints are jms ones.
 * Subclasses must provide the service configuration through the implementation
 * of {@link org.mule.tck.junit4.FunctionalTestCase#getConfigResources()}.
 */
public abstract class AbstractJmsRequestFromScriptTestCase extends FunctionalTestCase
{

    /**
     * Requests jms message from inside a groovy script which run as part of a
     * service defined using jms endpoints. The first part of the test loads
     * a couple of jms message in a queue so the script will have data to
     * process.
     */
    @Test
    public void testRequestingMessageFromScript() throws Exception
    {
        MuleClient muleClient = new MuleClient(muleContext);

        // Sends data to process
        muleClient.send("vm://in", TEST_MESSAGE, null);
        muleClient.send("vm://in", TEST_MESSAGE, null);

        // Sends the signal to start the batch process
        muleClient.send("vm://startBatch", TEST_MESSAGE, null);

        // Checks that the batch has processed the two messages without error
        MuleMessage message = muleClient.request("jms://status.queue?connector=jmsConnector", 5000);
        assertNotNull(message);
        assertEquals("messagemessage", message.getPayloadAsString());
    }
}
