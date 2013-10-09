/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DynamicEndpointWithConnectorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "dynamic-endpoint-with-connector-config.xml";
    }

    @Test
    public void testDynamicEndpointAcceptsConnectorRef() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        String testMessage = "TEST";
        DefaultMuleMessage message = new DefaultMuleMessage(testMessage, muleContext);
        message.setOutboundProperty("queueName", "test.out");

        MuleMessage test = client.send("vm://input", message, null);
        assertNotNull(test);

        MuleMessage response = client.request("jms://test.out", 5000);
        assertEquals(testMessage, response.getPayload());
    }
}
