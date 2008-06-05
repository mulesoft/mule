/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.List;

public class InboundAggregationNoTimeoutTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/multi-inbound-aggregator-no-timeout.xml";
    }

    public void testAggregatorWithNoTimeout() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient();
        client.dispatch("vm://distributor.queue", message, null);

        MuleMessage result = client.request("vm://results", 10000);

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

    public static class TestCollectionService
    {
        public Object process(List responseMessages)
        {
            assertTrue(responseMessages.size() == 3);
            return responseMessages;
        }
    }
}