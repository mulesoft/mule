/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;

import org.junit.Test;

public class InboundAggregationNoTimeoutTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/multi-inbound-aggregator-no-timeout-flow.xml";
    }

    @Test
    public void testAggregatorWithNoTimeout() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test", muleContext);
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://distributor.queue", message);

        MuleMessage result = client.request("vm://results", 10000);

        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection mc = (MuleMessageCollection)result;
        assertEquals(3, mc.size());
        for (int i = 0; i < mc.getMessagesAsArray().length; i++)
        {
            MuleMessage msg = mc.getMessagesAsArray()[i];
            assertEquals("test Received", msg.getPayload());
            assertEquals(message.getMessageRootId(), msg.getMessageRootId());
        }
        assertEquals(message.getMessageRootId(), result.getMessageRootId());
    }

    public static class TestCollectionService implements Callable
    {
        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            MuleMessage message = eventContext.getMessage();
            process((List<?>) message.getPayload());
            return message;
        }

        public Object process(List<?> responseMessages)
        {
            assertEquals(3, responseMessages.size());
            return responseMessages;
        }
    }
}
