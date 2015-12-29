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

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.List;

import org.junit.Test;

public class AsyncReplyNoTimeoutTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/multi-async-repy-no-timeout.xml";
    }

    @Test
    public void testAggregatorWithNoTimeout() throws Exception
    {
        String message = "test";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://distributor.queue", message, null);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof List);

        List<MuleMessage> results = (List<MuleMessage>) result.getPayload();
        assertEquals(3, results.size());
        for (int i = 0; i < results.size(); i++)
        {
            MuleMessage msg = results.get(i);
            assertEquals("test Received", msg.getPayload());
        }
    }
}
