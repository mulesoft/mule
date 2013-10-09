/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.routing;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncReplyNoTimeoutTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-async-repy-no-timeout.xml";
    }

    @Test
    public void testAggregatorWithNoTimeout() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://distributor.queue", message, null);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection mc = (MuleMessageCollection)result;
        assertEquals(3, mc.size());
        for (int i = 0; i < mc.getMessagesAsArray().length; i++)
        {
            MuleMessage msg = mc.getMessagesAsArray()[i];
            assertEquals("test Received", msg.getPayload());
        }
    }
}
