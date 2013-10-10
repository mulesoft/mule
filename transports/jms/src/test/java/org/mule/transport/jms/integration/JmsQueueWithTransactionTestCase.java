/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JmsQueueWithTransactionTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-queue-with-transaction.xml";
    }

    @Test
    public void testOutboundJmsTransaction() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://in", new DefaultMuleMessage(DEFAULT_INPUT_MESSAGE, muleContext));

        MuleMessage response = client.request("vm://out", getTimeout());
        assertNotNull(response);
        assertEquals(DEFAULT_INPUT_MESSAGE, response.getPayloadAsString());
    }

}
