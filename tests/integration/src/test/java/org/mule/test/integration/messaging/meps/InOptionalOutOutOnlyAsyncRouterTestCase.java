/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class InOptionalOutOutOnlyAsyncRouterTestCase extends FunctionalTestCase
{
    public static final long TIMEOUT = 3000;

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNull(result);

        MuleMessage msg = getTestMuleMessage("some data");
        msg.setOutboundProperty("foo", "bar");
        result = client.send("inboundEndpoint", msg);
        assertNotNull(result);
        assertEquals("got it!", result.getPayloadAsString());

        FlowConstruct async = muleContext.getRegistry().lookupFlowConstruct(
                "In-Out_Out-Only-Async-Service");
        FlowConstruct external = muleContext.getRegistry().lookupFlowConstruct("ExternalApp");

        assertEquals(2, async.getStatistics().getProcessedEvents());
        assertEquals(1, external.getStatistics().getProcessedEvents());
    }
}
