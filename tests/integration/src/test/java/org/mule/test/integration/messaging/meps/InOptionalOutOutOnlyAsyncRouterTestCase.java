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
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.service.Service;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class InOptionalOutOutOnlyAsyncRouterTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long TIMEOUT = 3000;

    public InOptionalOutOutOnlyAsyncRouterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router-flow.xml"}

        });
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

        if (ConfigVariant.SERVICE.equals(variant))
        {
            Service async = muleContext.getRegistry().lookupService("In-Out_Out-Only-Async-Service");
            Service external = muleContext.getRegistry().lookupService("ExternalApp");

            assertEquals(2, async.getStatistics().getProcessedEvents());
            assertEquals(1, external.getStatistics().getProcessedEvents());
        }
        else
        {
            FlowConstruct async = muleContext.getRegistry().lookupFlowConstruct(
                "In-Out_Out-Only-Async-Service");
            FlowConstruct external = muleContext.getRegistry().lookupFlowConstruct("ExternalApp");

            assertEquals(2, async.getStatistics().getProcessedEvents());
            assertEquals(1, external.getStatistics().getProcessedEvents());
        }

    }
}
