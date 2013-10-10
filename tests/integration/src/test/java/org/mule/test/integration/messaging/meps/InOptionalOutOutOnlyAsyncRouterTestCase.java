/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.service.Service;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

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
        MuleClient client = new MuleClient(muleContext);

        MuleMessage result = client.send("inboundEndpoint", "some data", null);
        assertNotNull(result);
        assertEquals(NullPayload.getInstance(), result.getPayload());
        assertNull(result.getExceptionPayload());

        DefaultMuleMessage msg = new DefaultMuleMessage("some data", muleContext);
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
